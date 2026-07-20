# IRON CHEF v2: MEMBER BACKLOG (THE EXECUTION ENGINE)

> **Track:** Systems & Computer Vision (Perception Engine)
> **Mandate:** Focus on real-time object detection, hardware optimization, and full-stack systems integration. You own the "Eyes" and the "Nervous System".

## EPIC 1: Layer 1 — The Perception Baseline (YOLO vs RT-DETR)
*Building the eyes of the cooking copilot.*

- [ ] **Data Pipeline Architecture:** Engineer the Kaggle templates and automation scripts to reliably download and unpack Food-101, Recipes5k, RecipeGen, and Roboflow FOOD-INGREDIENTS.
- [ ] **Taxonomy Validation:** Ensure the Roboflow dataset labels map correctly to our Phase 1 `taxonomy.yaml`.
- [ ] **YOLO Training:** Run the training loops for YOLOv8n and YOLOv11n on the FOOD-INGREDIENTS dataset. Export step reports with mAP@50 and mAP@50:95.
- [ ] **RT-DETR Training:** Run the training loop for the RT-DETR-L challenger model. Compare metrics against the YOLO baselines.
- [ ] **ONNX Export & Benchmarking:** Export the winning model to ONNX INT8. Write a benchmark script to measure inference latency on CPU (Target: < 100ms).

## EPIC 2: Perception Scaffold Wiring
*Connecting the models to the services.*

- [ ] **Remove the Stubs:** In `personal/projects/chefkix/perception/`, replace the training stubs with the actual model training code.
- [ ] **Endpoint Implementation:** Wire the ONNX inference endpoint into the `chefkix-ai-service` FastAPI structure. Ensure it returns standardized bounding boxes matching the taxonomy.
- [ ] **Evaluation Integration:** Run the `evaluate.py` script on the real models. Generate the final validation JSON reports.

## EPIC 3: Voice-Vision Copilot Frontend Wiring (The JARVIS Interface)
*Connecting the frontend sensors to the intelligence backend.*

- [ ] **Camera Integration:** In `chefkix-fe/src/components/cooking/CookingPanel.tsx`, capture camera frames as Base64 JPEGs and transmit them to the backend Layer 1 endpoint over standard WebSocket (avoids complex WebRTC termination on the Python backend).
- [ ] **Voice State Management:** Connect `useVoiceMode.ts` state to trigger the Graph-RAG pipeline. Package the audio command + the latest Base64 camera frame.
- [ ] **TTS Execution:** Implement the Text-To-Speech (TTS) hook to play the VLM's string response back to the user via the `KitchenAudioCoordinator`.

## EPIC 4: Hardware & Compute Support
*Keeping the engines running.*

- [ ] **Compute Cluster Management:** Orchestrate the training workloads. Manage Kaggle T4 schedules to avoid idle timeouts.
- [ ] **A100 Burst Execution Architecture (3-Day Window):** 
  - Engineer the environment setup for the A100.
  - Coordinate the massive hyperparameter sweeps for the HGAT.
  - Automate the checkpoint backup to remote storage.

## EPIC 5: Thesis Engineering Chapters
*Documenting the systems engineering.*

- [ ] **Chapter 4 (Perception):** Write the comparative analysis of YOLO vs RT-DETR. Include all metric tables and latency graphs.
- [ ] **Chapter 10 (Deployment & Integration):** Document the frontend wiring, ONNX CPU optimization, and the voice-vision copilot data flow.
- [ ] **Chapter 11 (Evaluation Support):** Compile the final metric tables for the thesis appendix.
