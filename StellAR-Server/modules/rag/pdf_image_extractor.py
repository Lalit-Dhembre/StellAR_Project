import os
import uuid
import logging
from io import BytesIO
from typing import List, Dict, Any

try:
    import fitz  # PyMuPDF
except ImportError:
    fitz = None

logger = logging.getLogger(__name__)


def _get_supabase_client():
    """
    Get the Supabase client via the shared SupabaseService singleton.
    This avoids creating a duplicate client and ensures consistent initialization.
    """
    try:
        from modules.supabase_service import supabase_service
        if not supabase_service.initialized:
            supabase_service.initialize()
        if not supabase_service.initialized:
            logger.error("Supabase service failed to initialize")
            return None
        return supabase_service.get_client()
    except Exception as e:
        logger.error(f"Cannot get Supabase client: {e}")
        return None


def _upload_bytes_to_supabase(supabase, bucket_name: str, file_path: str,
                               image_bytes: bytes, content_type: str) -> str:
    """
    Upload image bytes to Supabase Storage and return the public URL.
    Uses a BytesIO wrapper so the supabase-py SDK receives a file-like object
    (matching the pattern used in supabase_service.upload_file).
    """
    file_obj = BytesIO(image_bytes)
    
    supabase.storage.from_(bucket_name).upload(
        path=file_path,
        file=file_obj,
        file_options={"content-type": content_type}
    )
    
    public_url = supabase.storage.from_(bucket_name).get_public_url(file_path)
    return public_url


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

    supabase = _get_supabase_client()
    if supabase is None:
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
                
                # Normalize extension for content-type
                if image_ext == "jpg":
                    content_type = "image/jpeg"
                else:
                    content_type = f"image/{image_ext}"
                
                # Skip extremely tiny images (usually artifacts/icons)
                if len(image_bytes) < 1000:
                    continue
                
                # Generate unique filename
                filename = f"pdf_extract_{uuid.uuid4().hex[:8]}.{image_ext}"
                file_path = f"extracted/{filename}"
                
                # Upload to Supabase bucket
                public_url = _upload_bytes_to_supabase(
                    supabase, bucket_name, file_path,
                    image_bytes, content_type
                )
                
                uploaded_images.append({
                    "id": f"native-{uuid.uuid4().hex[:8]}",
                    "title": f"Figure from Page {page_index + 1}",
                    "image_url": public_url,
                    "page": page_index + 1,
                    "source": "pdf_native"
                })
                
                logger.info(f"Extracted and uploaded PDF image: {filename} ({len(image_bytes)} bytes)")
                
            except Exception as e:
                logger.warning(f"Failed to process image {img_index} on page {page_index + 1}: {e}")
                
    pdf_document.close()
    logger.info(f"PDF image extraction complete: {len(uploaded_images)} image(s) from {pdf_path}")
    return uploaded_images

def upload_raw_image(image_path: str, bucket_name: str = "images") -> List[Dict[str, Any]]:
    """
    Directly uploads a raw .jpg/.png image (e.g. from Camera) to Supabase 
    and wraps it in a single NativeImage dict so the UI can render it.
    """
    if not os.path.exists(image_path):
        return []
        
    supabase = _get_supabase_client()
    if supabase is None:
        return []
        
    try:
        with open(image_path, "rb") as f:
            image_bytes = f.read()
            
        ext = os.path.splitext(image_path)[1].lower().replace('.', '')
        if ext == 'jpg':
            ext = 'jpeg'
        if not ext:
            ext = 'jpeg'
        
        filename = f"camera_scan_{uuid.uuid4().hex[:8]}.{ext}"
        file_path = f"extracted/{filename}"
        
        public_url = _upload_bytes_to_supabase(
            supabase, bucket_name, file_path,
            image_bytes, f"image/{ext}"
        )
        
        logger.info(f"Uploaded raw camera image: {filename} ({len(image_bytes)} bytes)")
        
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
