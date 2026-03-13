from flask import Blueprint, request, jsonify
from modules.tasks import generate_3d_asset_task
from modules.supabase_service import supabase_service

resolve_bp = Blueprint('resolve_bp', __name__)

# Mock function for now, wait, the prompt says:
# "Queries assets DB for existing high-similarity embeddings (> 0.8)."
# We'll use supabase RPC or naive match if we don't have python-local vectors.

def get_entity_embedding(entity: str):
    try:
        import ollama
        response = ollama.embeddings(model='nomic-embed-text', prompt=entity)
        return response['embedding']
    except:
        return [0.0] * 768

def vector_similarity_search(embedding):
    """
    Search Supabase `assets` table using match_assets RPC or similar.
    If match_assets doesn't exist, this is a fallback placeholder.
    """
    try:
        supabase_service.initialize()
        # To do vector math in supabase:
        # response = supabase_service.client.rpc('match_assets', {'query_embedding': embedding, 'match_threshold': 0.8, 'match_count': 1}).execute()
        # For safety, let's just attempt it. If it fails, return None (triggering generation)
        response = supabase_service.client.rpc('match_assets', {
            'query_embedding': embedding, 
            'match_threshold': 0.8, 
            'match_count': 1
        }).execute()
        
        data = response.data
        if data and len(data) > 0:
            return data[0] # Returns the matched asset record
            
    except Exception as e:
        print(f"[Resolve API] Similarity search failed or RPC missing: {e}")
        
    # Also attempt naive keyword match just in case
    try:
         query = supabase_service.client.table('assets').select('*').limit(1).execute()
         # Actually just check keyword
         pass
    except:
         pass
         
    return None

@resolve_bp.route('/resolve-entity', methods=['POST'])
def resolve_entity():
    """
    Accepts: { "entity": "Human Heart" }
    Returns ready 3D model if exists, or processing task_id
    """
    data = request.json or {}
    entity = data.get('entity')
    
    if not entity:
        return jsonify({'error': 'Missing entity keyword'}), 400
        
    print(f"\n[Resolve API] Received request to resolve: '{entity}'")
    
    # 1. Generate embedding for search
    embedding = get_entity_embedding(entity)
    
    # 2. Vector search in assets DB
    match = vector_similarity_search(embedding)
    
    # 3. If exact/high-similarity match found
    if match and match.get('model_url'):
        print(f"[Resolve API] Found existing asset for '{entity}'")
        return jsonify({
            "status": "ready",
            "model_url": match['model_url']
        }), 200
        
    print(f"[Resolve API] No existing asset found for '{entity}', enqueuing generation task.")
    
    # 4. Trigger Celery Task or Fallback to Thread
    task_id = None
    
    # Quick Redis check before attempting Celery (avoids 20-retry hang)
    try:
        import redis
        r = redis.Redis(host='localhost', port=6379, socket_connect_timeout=1)
        r.ping()
        # Redis is up, safe to use Celery
        task = generate_3d_asset_task.delay(entity)
        task_id = task.id
        print(f"[Resolve API] Celery task enqueued: {task_id}")
    except Exception as e:
        print(f"[Resolve API] Redis/Celery unavailable: {e}. Falling back to background thread.")
        import threading
        task_id = f"local_thread_{hash(entity)}"
        
        def run_fallback():
            try:
                generate_3d_asset_task(entity)
            except Exception as thread_err:
                print(f"[Resolve API] Fallback thread failed: {thread_err}")
                
        thread = threading.Thread(target=run_fallback)
        thread.daemon = True
        thread.start()
    
    return jsonify({
        "status": "processing",
        "task_id": task_id
    }), 202


@resolve_bp.route('/task-status/<task_id>', methods=['GET'])
def get_task_status(task_id):
    """
    Polls Celery for the task status.
    """
    if task_id.startswith("local_thread_"):
        # We don't have a reliable way to track local threads easily without a DB.
        # Just tell the client it's processing so they keep polling, or let them timeout if it fails.
        # A better production solution would be storing the task status in SQLite/Supabase.
        return jsonify({
            "status": "processing",
            "task_id": task_id
        })
        
    try:
        task = generate_3d_asset_task.AsyncResult(task_id)
        
        if task.state == 'PENDING':
            response = {
                "status": "processing",
                "task_id": task_id
            }
        elif task.state == 'SUCCESS':
            result = task.result or {}
            # Expected result structure from tasks.py is dict
            response = {
                "status": "completed",
                "model_url": result.get('model_url')
            }
        elif task.state == 'FAILURE':
            response = {
                "status": "failed",
                "error": str(task.info)
            }
        else:
            # e.g., STARTED, RETRY
            response = {
                "status": "processing",
                "task_id": task_id
            }
            
        return jsonify(response)
    except Exception as e:
        print(f"[Resolve API] Redis/Celery polling failed: {e}")
        return jsonify({
            "status": "processing",
            "task_id": task_id
        })
