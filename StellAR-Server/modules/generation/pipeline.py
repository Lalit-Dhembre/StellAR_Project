import os
import requests
import uuid
import io
import time
import json
from PIL import Image
from flask import current_app
from modules.rag.image_retriever import retrieve_images

def fetch_wikipedia_image(entity: str) -> str:
    """
    Legacy wrapper kept for existing callers.
    Uses the production image retriever and returns only the image URL.
    """
    result = retrieve_images({"title": entity, "keywords": [entity]})
    return result.get("image_url")

def validate_image(image_url: str):
    """
    Reject if:
    - image resolution < 256px
    - file type is SVG
    - image is missing
    Downloads and returns local file path if validated.
    """
    if not image_url:
        return None
        
    if image_url.lower().endswith(".svg"):
        print(f"[Validation] Rejected: SVG format not supported -> {image_url}")
        return None
        
    try:
        headers = {
            "User-Agent": "StellAR/1.0 (https://github.com/Lalit-Dhembre/StellAR_Project; contact@stellar.app)"
        }
        response = requests.get(image_url, stream=True, headers=headers, timeout=10)
        response.raise_for_status()
        
        content = response.content
        img = Image.open(io.BytesIO(content))
        width, height = img.size
        
        if width < 256 or height < 256:
            print(f"[Validation] Rejected: Resolution too low ({width}x{height}) -> {image_url}")
            return None
            
        ext = os.path.splitext(image_url)[1].split('?')[0]
        if not ext:
            ext = ".jpg"
            
        # Ensure output temp dir exists
        temp_dir = os.path.join(os.getcwd(), 'temp_uploads')
        os.makedirs(temp_dir, exist_ok=True)
            
        temp_path = os.path.join(temp_dir, f"wiki_img_{uuid.uuid4().hex}{ext}")
        img.save(temp_path)
        print(f"[Validation] Success: Downloaded to {temp_path}")
        return temp_path
        
    except Exception as e:
        print(f"[Validation] Error validating {image_url}: {e}")
        return None

def generate_model_from_image(image_path: str, entity: str, app_context) -> str:
    """
    Call the Hunyuan 3D generation pipeline using image.
    Outputs a GLB mesh.
    """
    from modules.api.learning_modules import execute_model_generation
    prompt = f"Generate a simplified educational 3D model of a {entity} suitable for AR visualization for high school students. Focus on clear shape and smooth surfaces."
    print(f"[Generation - Image] Prompt context: {prompt}")
    
    start_time = time.time()
    try:
        # execute_model_generation currently handles the comfyui workflow mapping the image
        job_id = uuid.uuid4().hex
        result_path = execute_model_generation(app_context, job_id, image_path)
        print(f"[Generation - Image] Completed in {time.time() - start_time:.2f}s")
        return result_path
    except Exception as e:
        print(f"[Generation - Image] Failed after {time.time() - start_time:.2f}s: {e}")
        raise e

def generate_model_from_text(entity: str, app_context) -> str:
    """
    Call Hunyuan using text prompt via local SD1.5 text-to-image → Hunyuan3D image-to-3D pipeline.
    """
    import shutil

    prompt = f"A detailed high quality 3D render of {entity}, clean background, centered, single object, studio lighting"
    print(f"[Generation - Text] Prompt: {prompt}")
    
    start_time = time.time()
    try:
        comfy = app_context.comfy_client
        if not comfy:
            raise RuntimeError("ComfyUI client not initialized")

        # Load the text-to-3D workflow
        wf_path = os.path.join('workflows', 'hunyuan_text_to_3d_local_api.json')
        if not os.path.exists(wf_path):
            raise FileNotFoundError(f"Text-to-3D workflow not found: {wf_path}")

        with open(wf_path, 'r') as f:
            workflow = json.load(f)

        # Inject the user's text prompt into the positive prompt node
        for node in workflow.values():
            ct = node.get('class_type', '')
            meta_title = node.get('_meta', {}).get('title', '')
            if ct == 'CLIPTextEncode' and 'Positive' in meta_title:
                node['inputs']['text'] = prompt

        # Set unique filename prefix
        job_id = uuid.uuid4().hex[:12]
        target_prefix = f"text3d_{job_id}"
        for node in workflow.values():
            if 'filename_prefix' in node.get('inputs', {}):
                node['inputs']['filename_prefix'] = target_prefix

        # Queue the prompt
        result = comfy.queue_prompt(workflow)
        if not result:
            raise RuntimeError("Failed to queue text-to-3D prompt in ComfyUI")

        print(f"[Generation - Text] Prompt queued (prefix={target_prefix})")

        # Wait for the output GLB
        comfy_output_dir = app_context.config.get('COMFYUI_OUTPUT_DIR',
                                                   'D:/Coding/ComfyUI_windows_portable/ComfyUI/output/')
        import glob as glob_mod
        search_pattern = os.path.join(comfy_output_dir, f"{target_prefix}*.glb")
        final_glb = comfy.wait_for_completion(search_pattern)

        if not final_glb:
            raise RuntimeError(f"Text-to-3D generation timed out for '{entity}'")

        # Move to generated_models/
        gen_dir = app_context.config.get('GENERATED_DIR', 'generated_models')
        os.makedirs(gen_dir, exist_ok=True)
        filename = os.path.basename(final_glb)
        dest_path = os.path.join(gen_dir, filename)
        shutil.move(final_glb, dest_path)

        print(f"[Generation - Text] Completed in {time.time() - start_time:.2f}s -> {dest_path}")
        return dest_path

    except Exception as e:
        print(f"[Generation - Text] Failed after {time.time() - start_time:.2f}s: {e}")
        raise e

