echo "Starting model training..."

cd "$(dirname "$0")/.." || exit

./gradlew run --args="train"

echo "Training complete! Model saved to models/ directory"
