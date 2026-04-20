import os
import uuid
import logging
from typing import List, Dict, Any

try:
    import fitz  # PyMuPDF
except ImportError:
    fitz = None

from supabase import create_client, Client

logger = logging.getLogger(__name__)

# Cache the client so we don't recreate it every time
_supabase_client: Client = None

def get_supabase_client() -> Client:
    global _supabase_client
    if _supabase_client is None:
        url = os.environ.get("SUPABASE_URL")
        key = os.environ.get("SUPABASE_KEY")
        if not url or not key:
            raise ValueError("SUPABASE_URL and SUPABASE_KEY must be set in environment.")
        _supabase_client = create_client(url, key)
    return _supabase_client

def extract_and_upload_pdf_images(pdf_path: str, bucket_name: str = "images") -> List[Dict[str, Any]]:
    """
    Extracts all embedded images from a PDF and uploads them to Supabase.
    Returns a list of dicts with public URLs and page numbers.
    """
    if fitz is None:
        logger.error("PyMuPDF (fitz) is not installed. Cannot extract native PDF images.")
        return []

    if not pdf_path.lower().endswith(".pdf"):
        return []

    try:
        supabase = get_supabase_client()
    except Exception as e:
        logger.error(f"Cannot initialize Supabase client for image upload: {e}")
        return []

    uploaded_images = []
    
    try:
        pdf_document = fitz.open(pdf_path)
    except Exception as e:
        logger.error(f"Failed to open PDF for image extraction: {e}")
        return []

    for page_index in range(len(pdf_document)):
        page = pdf_document[page_index]
        image_list = page.get_images(full=True)
        
        for img_index, img in enumerate(image_list):
            xref = img[0]
            try:
                base_image = pdf_document.extract_image(xref)
                image_bytes = base_image["image"]
                image_ext = base_image["ext"]
                
                # We skip extremely tiny images which are usually artifacts/icons (e.g. < 1kb)
                if len(image_bytes) < 1000:
                    continue
                
                # Generate unique filename
                filename = f"pdf_extract_{uuid.uuid4().hex[:8]}.{image_ext}"
                file_path = f"extracted/{filename}"
                
                # Upload to Supabase bucket
                response = supabase.storage.from_(bucket_name).upload(
                    file_path,
                    image_bytes,
                    {"content-type": f"image/{image_ext}"}
                )
                
                # Get public URL
                public_url = supabase.storage.from_(bucket_name).get_public_url(file_path)
                
                uploaded_images.append({
                    "id": f"native-{uuid.uuid4().hex[:8]}",
                    "title": f"Figure from Page {page_index + 1}",
                    "image_url": public_url,
                    "page": page_index + 1,
                    "source": "pdf_native"
                })
                
                logger.info(f"Successfully extracted and uploaded PDF image: {filename}")
                
            except Exception as e:
                logger.warning(f"Failed to process image {img_index} on page {page_index}: {e}")
                
    pdf_document.close()
    return uploaded_images

def upload_raw_image(image_path: str, bucket_name: str = "images") -> List[Dict[str, Any]]:
    """
    Directly uploads a raw .jpg/.png image (e.g. from Camera) to Supabase 
    and wraps it in a single NativeImage dict so the UI can render it.
    """
    if not os.path.exists(image_path):
        return []
        
    try:
        supabase = get_supabase_client()
    except Exception as e:
        logger.error(f"Cannot initialize Supabase client for raw image upload: {e}")
        return []
        
    try:
        with open(image_path, "rb") as f:
            image_bytes = f.read()
            
        ext = os.path.splitext(image_path)[1].lower().replace('.', '')
        if ext == 'jpg': ext = 'jpeg'
        if not ext: ext = 'jpeg'
        
        filename = f"camera_scan_{uuid.uuid4().hex[:8]}.{ext}"
        file_path = f"extracted/{filename}"
        
        response = supabase.storage.from_(bucket_name).upload(
            file_path,
            image_bytes,
            {"content-type": f"image/{ext}"}
        )
        
        public_url = supabase.storage.from_(bucket_name).get_public_url(file_path)
        
        logger.info(f"Successfully uploaded raw camera image: {filename}")
        
        return [{
            "id": f"native-{uuid.uuid4().hex[:8]}",
            "title": "Camera Scan",
            "image_url": public_url,
            "page": 1,
            "source": "camera_scan"
        }]
    except Exception as e:
        logger.error(f"Failed to process and upload raw camera image: {e}")
        return []
