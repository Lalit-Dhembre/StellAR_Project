import requests
import json
import time
import os

BASE_URL = 'http://127.0.0.1:5000'

def test_pipeline():
    print("=== Testing RAG Orchestration Pipeline ===")
    
    # 1. Create a sample text file
    test_file_path = "temp_test_doc.txt"
    with open(test_file_path, "w") as f:
        f.write("The human heart is an organ that pumps blood throughout the body via the circulatory system. "
                "Photosynthesis is a process used by plants to convert light energy into chemical energy.")
    
    # 2. Test /process-content
    print("\n[1] Submitting file to /api/rag/process-content...")
    start_time = time.time()
    try:
        with open(test_file_path, 'rb') as f:
            files = {'file': f}
            data = {'expected_domain': 'biology'}
            res = requests.post(f"{BASE_URL}/api/rag/process-content", files=files, data=data)
            
        print(f"Elapsed: {time.time() - start_time:.2f}s | Status: {res.status_code}")
        if res.status_code != 200:
            print("Error:", res.text)
            return
            
        data = res.json()
        concepts = data.get("concepts", [])
        print(f"Found {len(concepts)} concepts:")
        for c in concepts:
            print(f"  - [{c.get('id')}] {c.get('title')} (Image: {'Yes' if c.get('image_url') else 'No'})")
            
        if not concepts:
            print("No concepts extracted. Test abort.")
            return
            
        # 3. Test /concept-details for the first concept
        target_concept = concepts[0]
        concept_id = target_concept['id']
        
        print(f"\n[2] Fetching details for {target_concept['title']} (ID: {concept_id})...")
        start_time = time.time()
        
        detail_res = requests.post(f"{BASE_URL}/api/rag/concept-details", json={"concept_id": concept_id})
        print(f"Elapsed: {time.time() - start_time:.2f}s | Status: {detail_res.status_code}")
        
        if detail_res.status_code == 200:
            detail_data = detail_res.json()
            print("Response:")
            print(json.dumps(detail_data, indent=2))
        else:
            print("Error:", detail_res.text)
            
    finally:
        if os.path.exists(test_file_path):
            os.remove(test_file_path)

if __name__ == "__main__":
    test_pipeline()
