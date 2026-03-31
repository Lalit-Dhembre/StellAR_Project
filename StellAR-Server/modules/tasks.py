import os
from celery import Celery
import uuid

# We will initialize this from app.py
celery_app = Celery(
    'stellar_tasks',
    backend=os.environ.get('CELERY_RESULT_BACKEND', 'redis://localhost:6379/0'),
    broker=os.environ.get('CELERY_BROKER_URL', 'redis://localhost:6379/0')
)

celery_app.conf.update(
    task_serializer='json',
    accept_content=['json'],
    result_serializer='json',
    timezone='UTC',
    enable_utc=True,
)

def generate_embedding(text: str):
    """
    Generate an embedding vector for the entity.
    """
    try:
        import ollama
        response = ollama.embeddings(model='all-minilm', prompt=text)
        return response['embedding'][:384] # Ensure 384 dimensions
    except Exception as e:
        print(f"[Tasks] Fallback embedding used for {text} due to: {e}")
        # Return a zero vector of typical dimensionality (384 for all-minilm)
        return [0.0] * 384

@celery_app.task(bind=True, name='modules.tasks.generate_3d_asset_task')
def generate_3d_asset_task(self, entity_name: str):
    """
    Background Task executing the 3D Generation Pipeline:
    1. Fetch Wikipedia Image
    2. Validate Image
    3. Generate 3D Model (Image or Text payload)
    4. Convert to GLB
    5. Upload to Supabase bucket
    6. Insert to 'assets' table
    """
    from modules.generation.pipeline import (
        fetch_wikipedia_image,
        validate_image,
        generate_model_from_image,
        generate_model_from_text
    )
    from modules.supabase_service import supabase_service
    from app import app
    
    print(f"\n[Celery] Starting generate_3d_asset_task for entity: '{entity_name}'")
    try:
        # Step 1: Fetch
        img_url = fetch_wikipedia_image(entity_name)
        
        # Step 2: Validate
        valid_img_path = validate_image(img_url) if img_url else None
        
        model_path = None
        
        # We need app context for Flask current_app configs
        with app.app_context():
            # Step 3 & 4: Generate Model
            if valid_img_path and os.path.exists(valid_img_path):
                print(f"[Celery] Valid image found, generating from image conditioned pipeline.")
                model_path = generate_model_from_image(valid_img_path, entity_name, app)
            else:
                print(f"[Celery] No valid image, generating from text-only pipeline.")
                model_path = generate_model_from_text(entity_name, app)
                
            if not model_path or not os.path.exists(model_path):
                raise Exception("Generation failed, model path invalid or missing.")
                
            print(f"[Celery] Generation Complete. File: {model_path}")
            
            # Step 5: Upload to Storage Bucket
            supabase_service.initialize()
            bucket_name = "models"
            dest_path = f"generated/{uuid.uuid4().hex}_{os.path.basename(model_path)}"
            
            print(f"[Celery] Uploading {model_path} to Storage Bucket '{bucket_name}/{dest_path}'")
            model_url = supabase_service.upload_file(bucket_name, model_path, dest_path)
            print(f"[Celery] Upload Success. Public URL: {model_url}")
            
            # Step 6: Save to assets table
            print(f"[Celery] Computing semantic embedding for '{entity_name}'")
            embedding = generate_embedding(entity_name)
            
            data = {
                "keyword": entity_name.lower().strip(),
                "embedding": embedding,
                "model_url": model_url,
                "source": "generated"
            }
            
            print(f"[Celery] Inserting metadata into 'assets' table")
            supabase_service.insert_record("assets", data)
            task_id = self.request.id if self.request and hasattr(self.request, 'id') else 'local'
            print(f"[Celery] Task {task_id} finished successfully!")
            
            return {"status": "completed", "model_url": model_url, "entity": entity_name}
            
    except Exception as e:
        print(f"[Celery] Task Failed: {e}")
        try:
            if self.request and hasattr(self.request, 'id') and self.request.id:
                self.update_state(state="FAILURE", meta={"error": str(e)})
        except Exception as update_err:
            print(f"[Celery] Failed to update state: {update_err}")
        raise e
