# StellAR: The Immersive AR Learning Platform 🌟

## What is StellAR?
StellAR is a cutting-edge Augmented Reality (AR) educational platform that bridges the gap between static textbooks and interactive 3D learning. By leveraging AI-powered Concept Extraction and Generative 3D technologies, StellAR brings complex subjects from biology, physics, space, and history to life right in your physical environment.

Instead of just reading about the human heart, a DNA helix, or the solar system, students can scan their textbook pages or documents. Our system automatically identifies the key educational concepts, generates interactive 3D models on-the-fly, and projects them into the user's room. Learning is further enhanced with a gamification system—discovering and generating new models rewards students with XP and assets of varying rarities (Common to Legendary).

## How It Works (The Idea)
1. **Scan & Analyze**: A student scans a PDF or takes a picture of their study material using the mobile app.
2. **Concept Extraction (RAG Pipeline)**: The backend uses Large Language Models (LLMs) to read the text and pinpoint core concepts.
3. **AI 3D Generation**: Concepts are passed to an asynchronous 3D generation pipeline (powered by ComfyUI and Hunyuan3D). If a 3D model doesn't already exist, the AI creates one and uploads it to the cloud (Supabase).
4. **AR Immersion**: The 3D model (`.glb`) is streamed to the AR viewer, where the student can physically walk around, rotate, and interact with the object.

## Repository Structure
The platform is designed with a microservices-inspired architecture spanning four distinct codebases:

* **`StellAR-Mobile/`** 
  The frontend Native Android app (Kotlin). It provides the user interface for document scanning, concept discovery, quizzes, and tracking gamification progress.
  
* **`StellAR-Server/`** 
  The Python/Flask backend powerhouse. It orchestrates the entire intelligence pipeline, featuring:
  * **Concept Extraction & Document Parsing**: Extracting domain-aware concepts.
  * **Asynchronous Processing**: Using Redis and Celery to manage long-running 3D generation queues without blocking the API.
  * **Database & Storage**: Utilizing Supabase to persist user profiles, asset metadata, and `.glb` files.

* **`StellAR-Unity/`** 
  The Unity-based Augmented Reality rendering engine. It is responsible for anchoring the 3D models in the real world and providing an interactive spatial interface.

* **`Stellar-Physics-Engine/`** 
  A dedicated module for simulating realistic physics behaviors within the AR space, ensuring that interactive science experiments feel authentic.

## Getting Started & Setup
If you are looking to run the backend and generation pipeline locally, please refer to the **[SERVER_SETUP_GUIDE.md](./SERVER_SETUP_GUIDE.md)** located in this root directory. 

It contains detailed step-by-step instructions covering:
- Installing prerequisites (Python, Docker, Redis).
- Launching the local ComfyUI server.
- Configuring your `.env` variables for Supabase and external APIs.
- Running the Flask server and exposing it via Ngrok.

## Contribution Guidelines
1. Fork the repository and create your feature branch.
2. Ensure you've documented any new API endpoints or dependencies.
3. Submit a Pull Request outlining your changes.

## Project Architecture
### System Architecture
<img width="915" height="2177" alt="mermaid-diagram" src="https://github.com/user-attachments/assets/3a3d0afb-ca70-4e3c-905f-8852c6a932bd" />

### Content Generation 
<img width="777" height="3520" alt="mermaid-diagram (1)" src="https://github.com/user-attachments/assets/b5065f04-29d1-43fd-9af9-484486d08383" />

### Physics Engine
<img width="605" height="2294" alt="mermaid-diagram (2)" src="https://github.com/user-attachments/assets/054cc8b8-288a-4ed2-a7dc-7994050f8972" />

### Chemistry Lab
<img width="534" height="1704" alt="mermaid-diagram (3)" src="https://github.com/user-attachments/assets/df24592d-14da-49b0-9400-faf9fda6a5b0" />


## Working Videos
https://github.com/user-attachments/assets/9a136fda-9277-4be3-8c41-15ba353cc1c4



https://github.com/user-attachments/assets/599e2b1a-9228-410b-a56e-9c5e7f647ba0






