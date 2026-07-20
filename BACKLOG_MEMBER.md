# IRON CHEF v2: MEMBER BACKLOG (THE EXECUTION ENGINE)

> **Role:** Execution Member, Perception Lead, Data Engineer.
> **Mandate:** Your job is to build the pipelines, run the benchmarks, and wire the hardware. The Lead relies on your execution speed so they can focus on the novel GNN and VLM architectures. Be painstaking, be precise, and build robust scaffolding.

## EPIC 1: Layer 1 — The Perception Baseline (YOLO vs RT-DETR)
*Building the eyes of the cooking copilot.*

- [ ] **Data Pipeline Automation:** Write simple Bash scripts / Kaggle templates to download Food-101, Recipes5k, RecipeGen, and Roboflow FOOD-INGREDIENTS. Automate the unpacking so it's a 1-click operation.
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

- [ ] **Camera Integration:** In `chefkix-fe/src/components/cooking/CookingPanel.tsx`, wire the camera feed to send frames (or extracted features) to the backend Layer 1 endpoint.
- [ ] **Voice State Management:** Connect `useVoiceMode.ts` state to trigger the Graph-RAG pipeline. When the user asks a question, package the audio command + the current camera frame.
- [ ] **TTS Execution:** Implement the Text-To-Speech (TTS) hook to play the VLM's string response back to the user via the `KitchenAudioCoordinator`.

## EPIC 4: Hardware & Compute Support
*Keeping the engines running.*

- [ ] **Kaggle T4 Babysitting:** Monitor the training runs. Manage the checkpoint downloads. Ensure we don't hit idle timeouts on Kaggle.
- [ ] **A100 Burst Execution Support (3-Day Window):** 
  - Prep the environment scripts for the A100.
  - Run the massive hyperparameter sweeps for the HGAT while the Lead monitors.
  - Automate the checkpoint backup to Google Drive / local storage.

## EPIC 5: Thesis Engineering Chapters
*Documenting the systems engineering.*

- [ ] **Chapter 4 (Perception):** Write the comparative analysis of YOLO vs RT-DETR. Include all metric tables and latency graphs.
- [ ] **Chapter 10 (Deployment & Integration):** Document the frontend wiring, ONNX CPU optimization, and the voice-vision copilot data flow.
- [ ] **Chapter 11 (Evaluation Support):** Compile the final metric tables for the thesis appendix.
