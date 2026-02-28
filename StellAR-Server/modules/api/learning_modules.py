from flask import Blueprint, request, jsonify, current_app, send_file
import os
import uuid
import json
from werkzeug.utils import secure_filename
from modules.appwrite_service import appwrite_service
import threading
import shutil

learning_modules_bp = Blueprint('learning_modules_bp', __name__)

ALLOWED_IMAGE_EXTENSIONS = {'png', 'jpg', 'jpeg', 'webp'}


def allowed_file(filename, allowed_extensions):
    if '.' not in filename:
        return False
    ext = filename.rsplit('.', 1)[1].lower().strip()
    return ext in allowed_extensions



def execute_model_generation(app, job_id, image_path):
    """
    Executes ComfyUI 3D model generation synchronously.
    Returns the path to the generated model file.
    """
    try:
        comfy = app.comfy_client
        
        # 1. Upload Input Image to ComfyUI (for processing)
        image_filename = comfy.upload_image(image_path)
        
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
            # Save to GENERATED_DIR
            dest_path = os.path.join(app.config['GENERATED_DIR'], filename)
            shutil.move(final_glb, dest_path)
            
            # Note: We are no longer uploading to Supabase or storing results in memory
            # The file path is returned directly
            
            return dest_path
                    
        raise Exception(f"Generation failed: Output file not found for job {job_id}")
            
    except Exception as e:
        print(f"Generation job {job_id} failed: {e}")
        raise e
    finally:
        if os.path.exists(image_path):
            try:
                os.remove(image_path)
            except Exception as e:
                print(f"Warning: Could not delete temp image {image_path}: {e}")


@learning_modules_bp.route('/api/generate_model', methods=['POST'])
def generate_model():
    """
    Generate a 3D model from an image (Synchronous).
    Steps:
    1. Accept image file upload.
    2. Trigger 3D model generation via ComfyUI.
    3. Wait for completion.
    4. Return the generated .glb file directly.
    """
    try:
        # 1. Validate Files
        print("Endpoint Hit: /api/generate_model (Synchronous)")
        if 'image' not in request.files:
            return jsonify({'error': 'Missing image file'}), 400

        image_file = request.files['image']

        if image_file.filename == '':
            return jsonify({'error': 'No image file selected'}), 400

        if not allowed_file(image_file.filename, ALLOWED_IMAGE_EXTENSIONS):
            return jsonify({'error': f'Invalid image file type: {image_file.filename}. Allowed: {", ".join(ALLOWED_IMAGE_EXTENSIONS)}'}), 400

        # 2. Save image temporarily
        job_id = str(uuid.uuid4())
        output_dir = current_app.config.get('OUTPUT_DIR', 'models')
        if not os.path.exists(output_dir):
            os.makedirs(output_dir)
            
        image_filename = secure_filename(image_file.filename)
        temp_image_path = os.path.join(output_dir, f"temp_gen_{job_id}_{image_filename}")
        image_file.save(temp_image_path)

        print(f"DEBUG: Processing image - {image_file.filename}, Job ID: {job_id}")

        # 3. Synchronous Generation
        try:
            print("starting execution...")
            generated_model_path = execute_model_generation(
                current_app._get_current_object(), 
                job_id, 
                temp_image_path
            )
            
            print(f"Generation successful: {generated_model_path}")
            
            return send_file(
                generated_model_path, 
                mimetype='model/gltf-binary',
                as_attachment=True,
                download_name=f"model_{job_id}.glb"
            )

        except Exception as gen_error:
            print(f"Generation failed: {gen_error}")
            return jsonify({'error': f"Generation failed: {str(gen_error)}"}), 500

    except Exception as e:
        return jsonify({'error': f"Server error: {str(e)}"}), 500
