#!/bin/bash

# Script to remove all comments from Java files
# WARNING: This will modify files in place

echo "Removing comments from Java files..."

# Function to remove comments from a Java file
remove_comments() {
    local file="$1"
    echo "Processing: $file"
    
    # Create a backup
    cp "$file" "$file.backup"
    
    # Remove comments using sed
    # 1. Remove single-line // comments
    # 2. Remove multi-line /* */ comments
    # 3. Remove JavaDoc /** */ comments
    
    # Use a more sophisticated approach with perl
    perl -i -pe '
        # Remove single-line comments
        s|//.*$||g;
    ' "$file"
    
    # Remove multi-line comments (including JavaDoc)
    perl -0777 -i -pe '
        # Remove /* ... */ and /** ... */
        s|/\*.*?\*/||gs;
    ' "$file"
    
    # Remove empty lines
    sed -i '/^[[:space:]]*$/d' "$file"
}

# Process all Java files in src
find src -name "*.java" -type f | while read -r file; do
    remove_comments "$file"
done

# Process all Java files in ml-model/src
find ml-model/src -name "*.java" -type f | while read -r file; do
    remove_comments "$file"
done

echo ""
echo "Done! Backups created with .backup extension"
echo "To restore backups: find . -name '*.backup' -exec bash -c 'mv \"\$0\" \"\${0%.backup}\"' {} \;"
