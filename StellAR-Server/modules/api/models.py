from flask import Blueprint, request, jsonify, send_file, current_app
from flask_jwt_extended import jwt_required, get_jwt_identity
from modules.models import db, Model, User
import os
import uuid
import time
import threading
import json

# Change prefix to /api to allow /api/models and /api/modelurl
models_bp = Blueprint('models', __name__, url_prefix='/api')

# --- 1. GET /api/models ---
@models_bp.route('/models', methods=['GET'])
def list_models():
    """List models filtered by subject"""
    try:
        from modules.supabase_service import supabase_service
        
        subject = request.args.get('subject')
        
        # Build query
        # We want speicific fields: model_id, model_name, description, rarity, model_subject, model_thumbnail, xp_reward
        # Supabase 'select' can do this.
        
        filters = {}
        if subject:
            filters['model_subject'] = subject
            
        models = supabase_service.query_records("models", select="*", filters=filters)
        
        # Transform logic if necessary, otherwise return as is.
        # The user requested specific JSON structure. Supabase returns list of dicts.
        # We can map it to be safe or return direct if columns match.
        # User requested: model_id, model_name, description, rarity, model_subject, model_thumbnail, xp_reward
        # Our schema has these exact columns.
        
        return jsonify(models)
        
    except Exception as e:
        print(f"❌ Error listing models: {e}")
        return jsonify({'error': str(e)}), 500

# --- 2. GET /api/modelurl ---
@models_bp.route('/modelurl', methods=['GET'])
def get_model_url():
    """Fetch specific 3D asset URL"""
    try:
        from modules.supabase_service import supabase_service
        
        model_id = request.args.get('model_id')
        if not model_id:
            return jsonify({'error': 'model_id is required'}), 400
            
        # Query Supabase: select model_url from models where model_id = model_id
        # query_records returns a list
        results = supabase_service.query_records("models", select="model_url", filters={"model_id": model_id})
        
        if not results:
            return jsonify({'error': 'Model not found'}), 404
            
        return jsonify(results[0])
        
    except Exception as e:
        print(f"❌ Error getting model url: {e}")
        return jsonify({'error': str(e)}), 500


# --- Legacy/Local Routes (Optional/Fallback) ---

@models_bp.route('/models/<int:model_id>/download', methods=['GET'])
@jwt_required()
def download_model(model_id):
    """(Legacy) Download local .glb file"""
    model = Model.query.get_or_404(model_id)
    # ... existing permissions logic ...
    user_id = int(get_jwt_identity())
    if not model.is_public and model.uploader_id != user_id:
        return jsonify({'error': 'Unauthorized'}), 403
    if not os.path.exists(model.file_path):
        return jsonify({'error': 'File not found on server'}), 404
    return send_file(model.file_path, as_attachment=True, download_name=f"{model.name}.glb")


# --- Generation Logic ---

# Store generation results for synchronous waiting
_generation_results = {}

@models_bp.route('/models/generate', methods=['POST'])
@models_bp.route('/generate_model', methods=['POST'])
def generate_model():
    """Trigger 3D generation task — synchronous: waits and returns .glb file"""
    # Accept both 'file' and 'image' field names from mobile
    file = request.files.get('file') or request.files.get('image')
    prompt = request.form.get('prompt', '')
    
    if not file and not prompt:
        return jsonify({'error': 'Either an image file or a text prompt is required'}), 400
        
    name_input = request.form.get('name') 
    subject_input = request.form.get('subject', 'Astronomy')
    
    comfy_client = current_app.comfy_client
    if not comfy_client:
        return jsonify({'error': 'Generation service unavailable'}), 503
        
    # Save temp input
    job_id = str(uuid.uuid4())
    output_dir = current_app.config.get('OUTPUT_DIR', 'models')
    
    input_path = None
    if file:
        input_path = os.path.join(output_dir, f"temp_gen_input_{job_id}.png")
        file.save(input_path)
    
    # Use a threading event to wait for completion
    done_event = threading.Event()
    _generation_results[job_id] = {'event': done_event, 'glb_path': None, 'error': None}

    thread = threading.Thread(target=run_generation_task, 
                            args=(current_app._get_current_object(), job_id, input_path, 0, name_input, subject_input, prompt))
    thread.daemon = True
    thread.start()
    
    # Wait synchronously for generation to complete (up to 15 minutes)
    done_event.wait(timeout=900)
    
    result = _generation_results.pop(job_id, None)
    if result and result.get('glb_path') and os.path.exists(result['glb_path']):
        return send_file(result['glb_path'], as_attachment=True, download_name=f"{name_input or 'model'}.glb")
    elif result and result.get('error'):
        return jsonify({'error': str(result['error'])}), 500
    else:
        return jsonify({'error': 'Generation timed out or failed'}), 504

def calculate_rarity():
    import random
    roll = random.random()
    if roll < 0.05: return "Legendary", 500
    elif roll < 0.20: return "Epic", 150
    elif roll < 0.50: return "Rare", 50
    else: return "Common", 10

def run_generation_task(app, job_id, image_path, user_id, user_provided_name=None, subject='Astronomy', prompt=None, metadata_override=None, uploader_id_override=None):
    """Background task for ComfyUI generation"""
    with app.app_context():
        dest_path = None
        try:
            comfy = app.comfy_client
            from modules.supabase_service import supabase_service
            thumbnail_url = ""
            
            if image_path and os.path.exists(image_path):
                # 1. Upload Input Image to ComfyUI (for processing)
                image_filename = comfy.upload_image(image_path)
                
                # --- SUPABASE: Upload Thumbnail ---
                if supabase_service.initialized:
                    try:
                        thumb_name = f"thumb_{job_id}.png"
                        thumbnail_url = supabase_service.upload_file("models", image_path, thumb_name)
                    except Exception as e:
                        print(f"⚠️ Thumbnail upload failed: {e}")

                # 2. Load Workflow
                wf_path = os.path.join('workflows', 'hunyuan_workflow_api.json')
                if not os.path.exists(wf_path):
                    wf_path = os.path.join('workflows', 'hunyuan_workflow.json')
                    
                with open(wf_path, 'r') as f:
                    workflow = json.load(f)
                    
                # 3. Modify Workflow
                for node in workflow.values():
                    if node.get('class_type') == 'LoadImage':
                        node['inputs']['image'] = image_filename
            else:
                # Text-to-3D (fully local: SD 1.5 text-to-image → Hunyuan3D image-to-3D)
                wf_path = os.path.join('workflows', 'hunyuan_text_to_3d_local_api.json')
                with open(wf_path, 'r') as f:
                    workflow = json.load(f)
                    
                # Inject the user's text prompt into the positive prompt node
                text_prompt = prompt if prompt else "A detailed 3d model, clean background, centered, studio lighting"
                for node in workflow.values():
                    if node.get('class_type') == 'CLIPTextEncode' and node.get('_meta', {}).get('title') == 'Positive Prompt':
                        node['inputs']['text'] = text_prompt

            target_prefix = f"gen_{job_id}"
            for node in workflow.values():
                if 'filename_prefix' in node.get('inputs', {}):
                    node['inputs']['filename_prefix'] = target_prefix
                    
            # 4. Queue & Wait
            comfy.queue_prompt(workflow)
            
            comfy_output_dir = app.config.get('COMFYUI_OUTPUT_DIR')
            search_pattern = os.path.join(comfy_output_dir, f"{target_prefix}*.glb")
            
            final_glb = comfy.wait_for_completion(search_pattern)
            
            if final_glb:
                filename = os.path.basename(final_glb)
                dest_path = os.path.join(app.config['GENERATED_DIR'], filename)
                import shutil
                shutil.move(final_glb, dest_path)
                
                final_name = user_provided_name if user_provided_name else f"Generated Model {job_id[:8]}"
                
                # --- SUPABASE INTEGRATION ---
                try:
                    if supabase_service.initialized:
                        model_url = supabase_service.upload_file("models", dest_path, filename)
                        print(f"✓ Uploaded Model to Supabase: {model_url}")
                        
                        rarity_name, xp_val = calculate_rarity()
                        
                        final_metadata = {
                            "job_id": job_id,
                            "prompt": prompt if prompt else "Generated via ComfyUI"
                        }
                        if metadata_override:
                            final_metadata.update(metadata_override)

                        final_uploader = str(uploader_id_override) if uploader_id_override else str(user_id)

                        record = {
                            "model_name": final_name,
                            "description": final_metadata.get('quiz_context', "Generated via ComfyUI"),
                            "model_url": model_url,
                            "rarity": rarity_name,
                            "xp_reward": xp_val,
                            "model_subject": subject,
                            "model_thumbnail": thumbnail_url,
                            "min_level": 1, 
                            "uploader_id": final_uploader,
                            "metadata": final_metadata
                        }
                        
                        supabase_service.insert_record("models", record)
                        print(f"✓ Record inserted into Supabase DB")
                    else:
                        print("⚠️ Supabase not initialized.")
                        
                except Exception as e:
                    print(f"⚠️ Supabase processing failed: {e}")

                # Signal success to the waiting endpoint
                if job_id in _generation_results:
                    _generation_results[job_id]['glb_path'] = dest_path
                    _generation_results[job_id]['event'].set()

            print(f"Job {job_id} complete: {dest_path}")
                
        except Exception as e:
            print(f"Job {job_id} failed: {e}")
            if job_id in _generation_results:
                _generation_results[job_id]['error'] = str(e)
                _generation_results[job_id]['event'].set()
        finally:
            if image_path and os.path.exists(image_path):
                os.remove(image_path)
