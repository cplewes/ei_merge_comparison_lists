#!/bin/bash
# Compare original decompiled files with modified versions to track changes

set -euo pipefail

PROJECT_ROOT="$(dirname "$(dirname "$(realpath "$0")")")"
ORIGINAL_DIR="$PROJECT_ROOT/original"
SRC_DIR="$PROJECT_ROOT/src"
OUTPUT_DIR="$PROJECT_ROOT/output/diffs"

# Colors for output
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[0;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo -e "${BLUE}🔍 Comparing Original vs Modified Files${NC}"
echo "========================================"

# Create output directory
mkdir -p "$OUTPUT_DIR"

# Function to compare a single file
compare_file() {
    local jar_id="$1"
    local relative_path="$2"
    local original_file="$ORIGINAL_DIR/$jar_id/src/$relative_path"
    local modified_file="$SRC_DIR/$jar_id/java/$relative_path"

    if [ ! -f "$original_file" ]; then
        echo -e "${YELLOW}⚠️  Original not found: $relative_path${NC}"
        return 1
    fi

    if [ ! -f "$modified_file" ]; then
        echo -e "${YELLOW}⚠️  Modified not found: $relative_path${NC}"
        return 1
    fi

    # Check if files are different
    if ! diff -q "$original_file" "$modified_file" >/dev/null 2>&1; then
        echo -e "${GREEN}📝 Changes found in: $jar_id/$relative_path${NC}"

        # Generate unified diff
        local diff_file="$OUTPUT_DIR/${jar_id}_$(basename "$relative_path" .java).diff"
        diff -u "$original_file" "$modified_file" > "$diff_file" || true

        # Show summary
        local lines_added=$(grep "^+" "$diff_file" | grep -v "^+++" | wc -l)
        local lines_removed=$(grep "^-" "$diff_file" | grep -v "^---" | wc -l)

        echo -e "  ${GREEN}+${lines_added}${NC} lines added, ${RED}-${lines_removed}${NC} lines removed"
        echo -e "  📄 Diff saved to: ${diff_file#$PROJECT_ROOT/}"

        # Show key changes (first few diff lines)
        echo -e "${YELLOW}  Key changes:${NC}"
        grep -E "^[+-]" "$diff_file" | grep -v -E "^[+-]{3}" | head -5 | sed 's/^/    /'
        if [ $(grep -E "^[+-]" "$diff_file" | grep -v -E "^[+-]{3}" | wc -l) -gt 5 ]; then
            echo "    ... (see full diff file for complete changes)"
        fi
        echo

        return 0
    else
        echo -e "${BLUE}✓ No changes: $jar_id/$relative_path${NC}"
        return 1
    fi
}

# Function to find all Java files in a JAR source directory
find_java_files() {
    local jar_id="$1"
    local src_java="$SRC_DIR/$jar_id/java"

    if [ ! -d "$src_java" ]; then
        echo "No source directory found for $jar_id"
        return
    fi

    find "$src_java" -name "*.java" -type f | while read -r file; do
        # Convert to relative path
        echo "${file#$src_java/}"
    done
}

# Main comparison logic
total_files=0
changed_files=0

echo -e "${BLUE}Scanning for modified Java files...${NC}"
echo

# Check each JAR
for jar_id in lta added; do
    if [ -d "$SRC_DIR/$jar_id/java" ]; then
        echo -e "${BLUE}=== JAR: $jar_id ===${NC}"

        while IFS= read -r relative_path; do
            total_files=$((total_files + 1))
            if compare_file "$jar_id" "$relative_path"; then
                changed_files=$((changed_files + 1))
            fi
        done < <(find_java_files "$jar_id")

        echo
    fi
done

# Summary
echo -e "${BLUE}📊 Summary${NC}"
echo "=========="
echo -e "Total files checked: ${total_files}"
echo -e "Files with changes: ${GREEN}${changed_files}${NC}"
echo -e "Files unchanged: $((total_files - changed_files))"

if [ $changed_files -gt 0 ]; then
    echo -e "\n${YELLOW}📁 Diff files saved in: ${OUTPUT_DIR#$PROJECT_ROOT/}${NC}"
    echo -e "Use 'git diff --no-index original/ src/' for full comparison"
fi