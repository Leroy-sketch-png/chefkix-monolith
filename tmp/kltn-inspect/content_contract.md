# IRON CHEF thesis sprint content contract

## Authority and scope

- Layout authority: `C:/Users/LENOVO/Downloads/PhanPhuTho_VoTrungTin_ Sprint2.docx` and its PDF rendering. Only its report layout, section rhythm, table pattern, and typography are reused.
- Content authority: `C:/Users/LENOVO/Downloads/De-cuong-chi-tiet-KLTN-v5.docx`, `C:/Users/LENOVO/Downloads/De-cuong-chi-tiet-KLTN-v5.pdf`, `chefkix-monolith/BACKLOG_LEAD.md`, and `chefkix-monolith/BACKLOG_MEMBER.md`.
- Project title: IRON CHEF / Multi-Source Knowledge Graph-Based Ingredient Substitution Recommendation System Combining Allergen Safety Control and Food Chemistry Explanations.
- Students: Phan Phú Thọ (23521520) and Võ Trung Tín (23521595).
- Supervisor: ThS. Trần Thị Hồng Yến.
- Thesis period: 07/09/2026 to 17/01/2027.

## Required subject matter

- Research question: unseen-pair generalization for ingredient substitution, not only repeated-pair accuracy.
- Core model: multi-source heterogeneous knowledge graph with HGAT and Feature-Adaptive Gating.
- Safety: Symbolic Safety Layer, FDA Big 9, EU 14, Open Food Facts, FDA recall data, allergen-safe filtering, and Selective Abstention.
- Explainability: FooDB and FlavorDB compound overlap, nutritional deltas, and user-facing food-chemistry explanations.
- Evaluation: Hit@1, Hit@5, Hit@10, MRR, NDCG, ablations, baselines (PMI, Epicure, GISMo, Gemini, Mistral), allergen safety benchmark, and User Study.
- Product integration: ChefKix Modular Monolith (Spring Boot), Next.js 15 frontend, FastAPI AI service, YOLOv8, CLIP, ONNX Runtime, MongoDB, Redis, Typesense, Keycloak, Kafka, Cloudinary, Docker.

## Sprint mapping

1. 07/09–22/09: evaluation and planning.
2. 23/09–10/10: dataset survey and requirements specification.
3. 11/10–28/10: system, graph, AI API, and UI/UX design.
4. 29/10–12/11: data acquisition, vocabulary, graph reconstruction, and baseline HGAT.
5. 13/11–26/11: multi-signal graph, HGAT v2 preparation, safety layer, and exports.
6. 27/11–10/12: HGAT v2 benchmarking, ablations, and chemical reasoning engine.
7. 11/12–24/12: allergen guard, AI service packaging, multimodal endpoints, and product integration.
8. 25/12–17/01: testing, experimental evaluation, deployment, thesis writing, and defense preparation.

## Backlog alignment

- Lead track maps to Lead Epics 1–9: graph reconstruction, HGAT v2, chemical reasoning, allergen safety, detection, behavioral feedback, flavor pairing, thesis evidence, and defense.
- Member track maps to Member Epics 1–12: graph explorer, evaluation dashboard, allergen profile, camera scaffold, compound explanation UI, allergen safety UI, feedback instrument, photo intelligence, real-data dashboard/graph wiring, thesis engineering, and optional Voice-Vision Copilot.
- Epic 12 Voice-Vision Copilot is explicitly Tier C and optional; it must not displace the thesis-critical Epics 1–11.
- The reports are prepared as schedule-aligned drafts. They use `Đang thực hiện`, `Theo kế hoạch`, `Chưa bắt đầu`, and `Hoàn thành` rather than claiming future work is already done.
