from flask import Blueprint, request, jsonify, current_app
from flask_jwt_extended import jwt_required, get_jwt_identity
import os
import uuid
from modules.models import db, User

# LangChain imports
from langchain_community.document_loaders import PyPDFLoader, TextLoader, UnstructuredImageLoader
from modules.api.domain_validator import validate_domain

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
        
        # 4. Construct Final Response (domain matched or no domain specified)
        response_data = {
            'success': True,
            'domain_match': True,
            'documents': all_documents,
            'count': len(all_documents)
        }
        print(f"[DEBUG] Success! Extracted {len(all_documents)} document sections.", flush=True)
        
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