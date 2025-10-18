# Models Directory

This directory contains trained ML models for fraud detection.

## Model Files

After training, you'll find:
- `fraud-detection-1.0-0000.params` - Model parameters (weights)
- `fraud-detection-1.0-symbol.json` - Model architecture (optional)

## Training a New Model

To train a new model:

```bash
cd ml-model
./gradlew run
```

Or use the training script:

```bash
cd ml-model
./scripts/train.sh
```

## Model Versioning

Models are versioned as `fraud-detection-{VERSION}-0000.params`

Default version: `1.0`

## Usage in Main Application

The main application loads the model from this directory.

Configure the path in `application.properties`:
```properties
app.ml.model-path=ml-model/models/fraud-detection-1.0-0000.params
```
