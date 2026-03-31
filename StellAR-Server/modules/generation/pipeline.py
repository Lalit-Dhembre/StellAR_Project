import os
import requests
import uuid
import io
import time
from PIL import Image
from flask import current_app

def fetch_wikipedia_image(entity: str) -> str:
    """
    Use the Wikipedia API to fetch an image for the entity.
    Returns the image URL.
    """
    url = "https://en.wikipedia.org/w/api.php"
    params = {
        "action": "query",
        "format": "json",
        "prop": "pageimages",
        "titles": entity,
        "pithumbsize": 500
    }
    
    headers = {
        "User-Agent": "StellAR/1.0 (https://github.com/Lalit-Dhembre/StellAR_Project; contact@stellar.app)"
    }
    
    try:
        response = requests.get(url, params=params, headers=headers, timeout=10)
        response.raise_for_status()
        data = response.json()
        pages = data.get("query", {}).get("pages", {})
        
        for page_id, page_data in pages.items():
            if "thumbnail" in page_data and "source" in page_data["thumbnail"]:
                return page_data["thumbnail"]["source"]
    except Exception as e:
        print(f"[Wikipedia] Error fetching image for {entity}: {e}")
        
    return None

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
        response = requests.get(image_url, stream=True, timeout=10)
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
    Call Hunyuan using text prompt only as fallback.
    """
    prompt = f"Generate a simplified educational 3D model of {entity} suitable for AR learning."
    print(f"[Generation - Text] Prompt context: {prompt}")
    
    start_time = time.time()
    try:
        # Assuming the backend comfy wrapper will be extended or text workflow exists. 
        # If the workflow currently doesn't support text, this will act as placeholder logic.
        print("[Generation - Text] Warning: Text-only pipeline currently not fully mapped in ComfyUI, falling back to basic execution if supported.")
        # We would invoke ComfyUI text workflow here.
        # Since we don't have text workflow mapped in execute_model_generation, we raise an exception for now
        # OR we can just simulate it. Let's raise NotImplementedError if text-only isn't properly wired yet.
        raise NotImplementedError("Text-to-3D workflow not found in current ComfyUI config")
    except Exception as e:
        print(f"[Generation - Text] Failed after {time.time() - start_time:.2f}s: {e}")
        raise e
