# Group 58 — Diabetes Patient Readmission Prediction

**Course module:** AI/ML Group Project
**Group ID:** 58

## Project overview

This project builds a machine-learning pipeline that predicts whether a diabetic patient will be
**readmitted to hospital within 30 days of discharge**, using the UCI "Diabetes 130-US hospitals for
years 1999–2008" dataset (101,766 encounters, 50 raw columns).

Each of the 8 group members individually owns, explains and justifies one preprocessing/EDA
technique (with dataset-specific evidence and a visualization) for the viva. Part 3 of the shared
pipeline then rebuilds the same ideas the correct, leakage-safe way for the actual model: split
first, fit every preprocessing step on the training data only, and never let the test set influence
training.

## Dataset

- `data/raw/diabetic_data.csv` — the assigned 101,766-encounter, 50-column dataset.
- `data/raw/IDS_mapping.csv` — decodes the `admission_type_id`, `discharge_disposition_id`, and
  `admission_source_id` integer codes (e.g. `discharge_disposition_id = 11` → "Expired"). This is
  used for more than readability: decoding `discharge_disposition_id` surfaces an important
  data-quality issue (deceased-patient encounters) that Member 1 flags and Part 3 removes before
  modelling.
- No external datasets were used.

## Group members, IT numbers, and assigned techniques

| # | Group Member Name | Student ID | Technique | Individual notebook |
|---|---|---|---|---|
| 1 | Venuvarshika .P | IT25104119 | Data understanding & initial EDA | `notebooks/IT25104119_data_understanding_initial_eda.ipynb` |
| 2 | Ekanayake E.W.T.G.B. | IT25104127 | Handling null/missing values | `notebooks/IT25104127_handling_null_missing_values.ipynb` |
| 3 | Kirinda G.W.R.W.M.T.D.W. | IT25510258 | Outlier detection & removal | `notebooks/IT25510258_outlier_detection_removal.ipynb` |
| 4 | Thilakarthna D.M.K.U. | IT25100879 | Label (ordinal) encoding | `notebooks/IT25100879_label_ordinal_encoding.ipynb` |
| 5 | Liyanage T.D.K. | IT25102251 | One-hot encoding | `notebooks/IT25102251_one_hot_encoding.ipynb` |
| 6 | Habishahini. M | IT25300081 | Feature scaling | `notebooks/IT25300081_feature_scaling.ipynb` |
| 7 | Meepalagama M.M.K.G.D. | IT25101804 | Feature engineering & selection | `notebooks/IT25101804_feature_engineering_selection.ipynb` |
| 8 | Rathnayake R.M.S.A. | IT25102851 | Target variable & class imbalance | `notebooks/IT25102851_target_variable_class_imbalance.ipynb` |

`notebooks/group_pipeline.ipynb` is the **combined, integrated pipeline**: all 8 members' techniques
re-expressed as a single leakage-safe `ColumnTransformer` + `SMOTE` + model pipeline, with proper
train/test splitting, cross-validated model comparison, threshold tuning against the university's
accuracy requirement, test-set evaluation, model persistence, and a single-patient inference
function.

## How to run

1. Open a notebook in Google Colab.
2. Install dependencies if needed: `pip install pandas numpy matplotlib seaborn scikit-learn imbalanced-learn xgboost joblib`.
3. Place `diabetic_data.csv` and `IDS_mapping.csv` (from `data/raw/`) in the same working directory
   as the notebook you're running (or upload them when the notebook prompts, in Colab).
4. Run all cells top to bottom. Each notebook automatically saves its figures into an
   `eda_visualizations/` folder in its working directory as it runs (already collected for you
   under `results/eda_visualizations/` in this submission).
5. `group_pipeline.ipynb` additionally saves the fitted preprocessor, model, feature names, and
   decision threshold as `.joblib` files, and exports the final processed feature matrix as
   `processed_diabetes_dataset.csv` (already collected under `results/outputs/`).

All 9 notebooks in this submission were executed end-to-end with **zero errors** before packaging.

## Key results (real numbers, from the executed `group_pipeline.ipynb`)

**Data cleaning:** removed 1,652 deceased-patient encounters (identified via
`IDS_mapping.csv`-decoded `discharge_disposition_id`, Member 1's finding) → 100,114 encounters
remain, 29 features. Train/test split: 80,091 / 20,023 (stratified, 80/20), positive class
("readmitted <30 days") rate 11.34% in both.

**Cross-validated model comparison (5-fold, selection metric = F1 on the readmit class):**

| Model | CV F1 (mean ± std) | CV ROC-AUC (mean ± std) | Test accuracy | Test precision | Test recall |
|---|---|---|---|---|---|
| Logistic Regression | 0.2611 ± 0.0046 | 0.6447 ± 0.0075 | 0.6545 | 0.1744 | 0.5482 |
| Random Forest | 0.0506 ± 0.0059 | 0.6415 ± 0.0091 | 0.8857 | 0.4423 | 0.0304 |
| XGBoost | 0.0538 ± 0.0029 | 0.6532 ± 0.0094 | 0.8861 | 0.4622 | 0.0242 |

Logistic Regression wins on F1 — the tree ensembles reach ~88.5–88.6% accuracy but collapse to
~2–3% recall on the imbalanced minority class (they mostly just predict "not readmitted").

**Meeting the university's ≥80% accuracy requirement:** the notebook deliberately overrides the
F1-based CV winner and deploys **Random Forest**, because Logistic Regression cannot clear an 80%
accuracy bar on this imbalanced target at any useful threshold. A decision threshold was searched
for on a validation split carved out of the training data only (never the test set), maximizing
recall subject to validation accuracy ≥ 80%:

- Chosen threshold: **0.250** (validation accuracy 0.811, validation recall 0.282)
- **Test set at threshold 0.250: accuracy = 0.8133, precision (readmit) = 0.23, recall (readmit) = 0.276, F1 (readmit) = 0.2512, ROC-AUC = 0.6464**
- For reference, the same Random Forest at the default 0.5 threshold: accuracy 0.885 but recall only 0.030 — i.e. it would flag almost none of the patients who actually get readmitted within 30 days. Lowering the threshold to 0.250 trades some accuracy for a model that actually catches ~28% of true readmissions.

Full per-cell output (including the confusion matrix and classification report) is in
`results/logs/group_pipeline_log.txt` and in the executed notebook itself.
