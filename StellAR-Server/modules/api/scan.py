from flask import Blueprint, request, jsonify, current_app
from flask_jwt_extended import jwt_required, get_jwt_identity
import os
import uuid
from modules.models import db, User

# LangChain imports
from langchain_community.document_loaders import PyPDFLoader, TextLoader, UnstructuredImageLoader
from modules.api.domain_validator import validate_domain, get_groq_client, GROQ_MODEL_NAME
from modules.generation.pipeline import fetch_wikipedia_image
import json

MAX_SECTIONS = 3

scan_bp = Blueprint('scan', __name__, url_prefix='/api/scan')

@scan_bp.route('', methods=['POST'])
# @jwt_required()
def scan_documents():
    """
    Upload documents -> OCR -> extract keywords -> Generate Info via LLM -> Return Combined Data
    """
    print("\n" + "="*50, flush=True) 
    print("[DEBUG] RECEIVED REQUEST: /api/scan docs", flush=True)
    temp_paths = []
    
    try:
        print(f"[DEBUG] Request Headers: {request.headers}", flush=True)
        
        # We expect a list of files under the key 'files' or 'file'
        files = request.files.getlist('files')
        if not files:
            files = request.files.getlist('file')
            
        if not files or files[0].filename == '':
            print("[DEBUG] ERROR: No files in request", flush=True)
            return jsonify({'error': 'No files uploaded'}), 400
            
        output_dir = current_app.config.get('OUTPUT_DIR', 'temp_uploads') 
        if not os.path.exists(output_dir):
            os.makedirs(output_dir)
            
        all_documents = []
        
        for file in files:
            print(f"[DEBUG] Processing File: '{file.filename}'", flush=True)
            temp_id = str(uuid.uuid4())
            ext = os.path.splitext(file.filename)[1].lower()
            temp_path = os.path.join(output_dir, f"temp_scan_{temp_id}{ext}")
            file.save(temp_path)
            temp_paths.append(temp_path)
            
            # Use LangChain document loaders based on extension
            try:
                if ext == '.pdf':
                    loader = PyPDFLoader(temp_path)
                elif ext == '.txt':
                    loader = TextLoader(temp_path)
                elif ext in ['.png', '.jpg', '.jpeg']:
                    # Requires unstructured and other dependencies
                    loader = UnstructuredImageLoader(temp_path)
                else:
                    print(f"[DEBUG] Unsupported file type: {ext}")
                    continue
                    
                docs = loader.load()
                # Convert LangChain Document objects to dicts for JSON serialization
                for doc in docs:
                    all_documents.append({
                        "page_content": doc.page_content,
                        "metadata": doc.metadata
                    })
                print(f"[DEBUG] Successfully loaded {len(docs)} documents from {file.filename}")
            except Exception as e:
                print(f"[DEBUG] Error loading {file.filename}: {e}")

        # --- Domain Validation ---
        domain = request.form.get('domain', '').strip().lower()
        print(f"[DEBUG] Requested domain: '{domain}'", flush=True)
        
        if domain and all_documents:
            # Concatenate text from all documents (first 2000 chars)
            full_text = "\n".join([d["page_content"] for d in all_documents])
            
            try:
                validation = validate_domain(full_text, domain)
                print(f"[DEBUG] Domain validation result: {validation}", flush=True)
                
                if not validation.get("match", False):
                    # Domain mismatch — tell the user
                    response_data = {
                        'success': False,
                        'domain_match': False,
                        'detected_domain': validation.get('detected_domain', 'unknown'),
                        'message': f"This document appears to be about {validation.get('detected_domain', 'another subject')}, not {domain}. Please upload a {domain}-related document.",
                        'reason': validation.get('reason', ''),
                        'documents': [],
                        'count': 0
                    }
                    print(f"[DEBUG] Domain mismatch! Returning rejection.", flush=True)
                    return jsonify(response_data), 200
            except Exception as e:
                print(f"[DEBUG] Domain validation error (proceeding anyway): {e}", flush=True)
        
        # 4. Extract Actual Sections with Groq
        actual_sections = []
        
        if all_documents:
            full_text = "\n".join([d["page_content"] for d in all_documents])
            # Truncate to reasonable length for Groq (~30k chars)
            text_excerpt = full_text[:30000]
            
            prompt = f"""You are analyzing a section of a high school science document.

Your task is to extract **physical objects that can be visualized as simplified 3D models** for AR learning.

Rules:

* Only extract **real physical objects**
* Ignore abstract concepts, processes, or theories
* Ignore sentences that do not describe objects
* Each result should represent **one clear physical entity**

Examples of valid entities:
Magnet
Electric Motor
Solar System
Human Heart
DNA Helix
Volcano
Atom Model

Examples of invalid entities:
Magnetism
Energy Transfer
Photosynthesis Process
Temperature Change
Scientific Theory

Note: Don't Repeat the name of sections and entities.
Respond ONLY with valid JSON in this format:

[
{{
"page_content": "<text related to the entity>",
"metadata": {{
"entity": "<physical object name>"
}}
}}
]

If no visualizable object exists in the text, return an empty array.

Text:
{text_excerpt}
"""
            
            try:
                client = get_groq_client()
                response = client.chat.completions.create(
                    model=GROQ_MODEL_NAME,
                    messages=[
                        {
                            "role": "system",
                            "content": "You are a precise document parser. Always respond with valid JSON only."
                        },
                        {
                            "role": "user", 
                            "content": prompt
                        }
                    ],
                    temperature=0.1,
                    max_completion_tokens=6000
                )
                
                result_text = response.choices[0].message.content.strip()
                if "```json" in result_text:
                    result_text = result_text.split("```json")[1].split("```")[0].strip()
                elif "```" in result_text:
                    result_text = result_text.split("```")[1].split("```")[0].strip()
                    
                actual_sections = json.loads(result_text)
                
                # Check format validity
                if not isinstance(actual_sections, list) or not all(isinstance(sec, dict) and "page_content" in sec for sec in actual_sections):
                    actual_sections = all_documents
                print(f"[DEBUG] Successfully extracted {len(actual_sections)} actual semantic sections via Groq.", flush=True)
                
            except Exception as e:
                print(f"[DEBUG] Failed to extract actual sections via Groq: {e}", flush=True)
                actual_sections = all_documents
        else:
            actual_sections = all_documents
            
        print("[DEBUG] Starting entity extraction for each section...", flush=True)
        entities_extracted = 0
        for section in actual_sections:
            if entities_extracted >= 3:
                if "metadata" not in section:
                    section["metadata"] = {}
                section["metadata"]["entity"] = None
                continue

            section_text = section.get("page_content", "")
            if not section_text:
                if "metadata" not in section:
                    section["metadata"] = {}
                section["metadata"]["entity"] = None
                continue

            entity_prompt = f"""You are fixing the entity extraction step in the StellAR backend.

Goal:
Extract a real physical object suitable for 3D visualization from each section of text.

Rules:
* Only extract physical objects
* Ignore abstract concepts
* Ignore scientific processes
* Ignore theories
* Ignore sentences
* Return only one best entity per section

Valid examples:
Magnet
Electric Motor
Human Heart
Solar System
Atom Model
Volcano
DNA Helix

Invalid examples:
Magnetism
Photosynthesis process
Energy transfer
Scientific theory
Temperature change

Note: Don't Repeat the name of sections and entities.

Output format must be strict JSON only:
{{
"entity": "<physical object name>",
"confidence": 0.0-1.0
}}

If no valid physical object exists return:
{{
"entity": null,
"confidence": 0.0
}}

Text section:
{section_text[:5000]}

Important implementation rules:
1. Never return placeholders like "Generated AR Topic".
2. Always return a real object name or null.
3. Strip extra text, explanations, or markdown.
4. Only return JSON."""

            try:
                client = get_groq_client()
                entity_response = client.chat.completions.create(
                    model=GROQ_MODEL_NAME,
                    messages=[
                        {
                            "role": "system",
                            "content": "You are a precise entity extractor. Always respond with valid JSON only."
                        },
                        {
                            "role": "user", 
                            "content": entity_prompt
                        }
                    ],
                    temperature=0.1,
                    max_completion_tokens=200
                )
                        
                entity_result_text = entity_response.choices[0].message.content.strip()
                if "```json" in entity_result_text:
                    entity_result_text = entity_result_text.split("```json")[1].split("```")[0].strip()
                elif "```" in entity_result_text:
                    entity_result_text = entity_result_text.split("```")[1].split("```")[0].strip()
                            
                entity_data = json.loads(entity_result_text)
                        
                entity_name = entity_data.get("entity")
                if "metadata" not in section:
                    section["metadata"] = {}
                section["metadata"]["entity"] = entity_name
                
                print(f"[DEBUG] Extracted entity: {entity_name}", flush=True)
                if entity_name:
                    entities_extracted += 1
                        
            except Exception as e:
                print(f"[DEBUG] Failed to extract entity via Groq: {e}", flush=True)
                if "metadata" not in section:
                    section["metadata"] = {}
                section["metadata"]["entity"] = None
            
        # 5. Filter: keep only sections with a valid entity name
        valid_sections = [
            sec for sec in actual_sections
            if sec.get("metadata", {}).get("entity")
        ]
        
        # Cap at MAX_SECTIONS
        valid_sections = valid_sections[:MAX_SECTIONS]
        print(f"[DEBUG] {len(valid_sections)} valid sections after filtering (max {MAX_SECTIONS}).", flush=True)
        
        # 6. Fetch Wikipedia images for each valid entity
        for section in valid_sections:
            entity_name = section["metadata"]["entity"]
            try:
                image_url = fetch_wikipedia_image(entity_name)
                section["metadata"]["image_url"] = image_url
                print(f"[DEBUG] Image for '{entity_name}': {image_url}", flush=True)
            except Exception as img_err:
                print(f"[DEBUG] Failed to fetch image for '{entity_name}': {img_err}", flush=True)
                section["metadata"]["image_url"] = None
        
        # 7. Construct Final Response
        response_data = {
            'success': True,
            'domain_match': True,
            'documents': valid_sections,
            'count': len(valid_sections),
            'max_sections': MAX_SECTIONS,
            'message': f'Only up to {MAX_SECTIONS} sections can be extracted from any given PDF.'
        }
        print(f"[DEBUG] Returning {len(valid_sections)} document sections.", flush=True)
        
        return jsonify(response_data), 200
        
    except Exception as e:
        import traceback
        traceback.print_exc()
        print(f"[DEBUG] CRITICAL ERROR in /api/scan: {e}", flush=True)
        return jsonify({'error': str(e)}), 500
        
    finally:
        for temp_path in temp_paths:
            if os.path.exists(temp_path):
                try:
                    os.remove(temp_path)
                    print(f"[DEBUG] Cleaned up temp file: {temp_path}", flush=True)
                except Exception as cleanup_err:
                    print(f"[DEBUG] Warning: Failed to cleanup temp file: {cleanup_err}", flush=True)