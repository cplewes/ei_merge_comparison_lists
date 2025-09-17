#!/bin/bash

# Multi-JAR Complete Build Pipeline
# This script compiles and rebuilds all configured JAR files

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Default settings
COMPILE_ONLY=false
REBUILD_ONLY=false
PARALLEL=false
VERBOSE=false

# Function to show usage
usage() {
    echo "Multi-JAR Build Pipeline"
    echo ""
    echo "Usage: $0 [OPTIONS] [JAR_IDS...]"
    echo ""
    echo "OPTIONS:"
    echo "  -c, --compile-only    Only compile, don't rebuild JARs"
    echo "  -r, --rebuild-only    Only rebuild JARs (skip compilation)"
    echo "  -p, --parallel        Build JARs in parallel (experimental)"
    echo "  -v, --verbose         Verbose output"
    echo "  -h, --help           Show this help message"
    echo ""
    echo "JAR_IDS (if not specified, builds all configured JARs):"
    echo "  lta                   LTA textarea components"
    echo "  added                 Added comparison studies (when configured)"
    echo ""
    echo "EXAMPLES:"
    echo "  $0                    Build all JARs (compile + rebuild)"
    echo "  $0 lta                Build only LTA JAR"
    echo "  $0 -c                 Compile all JARs only"
    echo "  $0 -r lta added       Rebuild only LTA and Added JARs"
    echo "  $0 -p                 Build all JARs in parallel"
}

# Function to log messages
log() {
    echo "$(date '+%H:%M:%S') 🔧 $*"
}

log_success() {
    echo "$(date '+%H:%M:%S') ✅ $*"
}

log_error() {
    echo "$(date '+%H:%M:%S') ❌ $*" >&2
}

log_warning() {
    echo "$(date '+%H:%M:%S') ⚠️  $*"
}

# Function to check if JAR is configured
is_jar_configured() {
    local jar_id="$1"
    case "$jar_id" in
        lta|added)
            return 0
            ;;
        *)
            return 1
            ;;
    esac
}

# Function to get available JAR IDs
get_available_jars() {
    local available_jars=()

    # Check which JARs are actually available
    if [ -f "$PROJECT_ROOT/jars/com.agfa.ris.client.lta_3090.0.13.release_20250728.jar" ]; then
        available_jars+=("lta")
    fi

    if [ -f "$PROJECT_ROOT/jars/com.agfa.ris.client.studylist.lta.added_3090.0.13.release_20250728.jar" ]; then
        available_jars+=("added")
    fi

    echo "${available_jars[@]}"
}

# Function to compile a single JAR
compile_jar() {
    local jar_id="$1"

    log "Compiling JAR: $jar_id"

    if [ -x "$PROJECT_ROOT/scripts/compile-jar.sh" ]; then
        if $VERBOSE; then
            "$PROJECT_ROOT/scripts/compile-jar.sh" "$jar_id"
        else
            "$PROJECT_ROOT/scripts/compile-jar.sh" "$jar_id" >/dev/null 2>&1
        fi

        if [ $? -eq 0 ]; then
            log_success "Compilation successful: $jar_id"
            return 0
        else
            log_error "Compilation failed: $jar_id"
            return 1
        fi
    else
        log_error "Compile script not found: $PROJECT_ROOT/scripts/compile-jar.sh"
        return 1
    fi
}

# Function to rebuild a single JAR
rebuild_jar() {
    local jar_id="$1"

    log "Rebuilding JAR: $jar_id"

    if [ -x "$PROJECT_ROOT/scripts/rebuild-jar.sh" ]; then
        if $VERBOSE; then
            "$PROJECT_ROOT/scripts/rebuild-jar.sh" "$jar_id"
        else
            "$PROJECT_ROOT/scripts/rebuild-jar.sh" "$jar_id" >/dev/null 2>&1
        fi

        if [ $? -eq 0 ]; then
            log_success "Rebuild successful: $jar_id"
            return 0
        else
            log_error "Rebuild failed: $jar_id"
            return 1
        fi
    else
        log_error "Rebuild script not found: $PROJECT_ROOT/scripts/rebuild-jar.sh"
        return 1
    fi
}

# Function to build a single JAR (compile + rebuild)
build_jar() {
    local jar_id="$1"
    local success=true

    log "Building JAR: $jar_id"

    # Compile phase
    if ! $REBUILD_ONLY; then
        if ! compile_jar "$jar_id"; then
            success=false
        fi
    fi

    # Rebuild phase
    if $success && ! $COMPILE_ONLY; then
        if ! rebuild_jar "$jar_id"; then
            success=false
        fi
    fi

    if $success; then
        log_success "Build complete: $jar_id"
    else
        log_error "Build failed: $jar_id"
    fi

    return $([ "$success" = true ] && echo 0 || echo 1)
}

# Function to build multiple JARs in parallel
build_jars_parallel() {
    local jar_ids=("$@")
    local pids=()
    local results=()

    log "Building ${#jar_ids[@]} JARs in parallel..."

    # Start background processes
    for jar_id in "${jar_ids[@]}"; do
        (
            build_jar "$jar_id"
            echo $? > "$PROJECT_ROOT/temp/build_result_$jar_id"
        ) &
        pids+=($!)
    done

    # Wait for all processes to complete
    for pid in "${pids[@]}"; do
        wait $pid
    done

    # Collect results
    local overall_success=true
    for jar_id in "${jar_ids[@]}"; do
        local result_file="$PROJECT_ROOT/temp/build_result_$jar_id"
        if [ -f "$result_file" ]; then
            local result=$(cat "$result_file")
            rm "$result_file"
            if [ "$result" -ne 0 ]; then
                overall_success=false
            fi
        else
            overall_success=false
        fi
    done

    return $([ "$overall_success" = true ] && echo 0 || echo 1)
}

# Function to build multiple JARs sequentially
build_jars_sequential() {
    local jar_ids=("$@")
    local overall_success=true

    log "Building ${#jar_ids[@]} JARs sequentially..."

    for jar_id in "${jar_ids[@]}"; do
        if ! build_jar "$jar_id"; then
            overall_success=false
            if ! $VERBOSE; then
                log_error "Build failed for $jar_id, stopping..."
                break
            fi
        fi
    done

    return $([ "$overall_success" = true ] && echo 0 || echo 1)
}

# Main function
main() {
    local jar_ids=()

    # Parse command line arguments
    while [[ $# -gt 0 ]]; do
        case $1 in
            -c|--compile-only)
                COMPILE_ONLY=true
                shift
                ;;
            -r|--rebuild-only)
                REBUILD_ONLY=true
                shift
                ;;
            -p|--parallel)
                PARALLEL=true
                shift
                ;;
            -v|--verbose)
                VERBOSE=true
                shift
                ;;
            -h|--help)
                usage
                exit 0
                ;;
            -*)
                log_error "Unknown option: $1"
                usage
                exit 1
                ;;
            *)
                if is_jar_configured "$1"; then
                    jar_ids+=("$1")
                else
                    log_error "Unknown JAR ID: $1"
                    log_warning "Available JARs: $(get_available_jars)"
                    exit 1
                fi
                shift
                ;;
        esac
    done

    # Validate conflicting options
    if $COMPILE_ONLY && $REBUILD_ONLY; then
        log_error "Cannot specify both --compile-only and --rebuild-only"
        exit 1
    fi

    # If no JAR IDs specified, use all available
    if [ ${#jar_ids[@]} -eq 0 ]; then
        IFS=' ' read -ra jar_ids <<< "$(get_available_jars)"
    fi

    # Check if any JARs are available
    if [ ${#jar_ids[@]} -eq 0 ]; then
        log_error "No JAR files found in jars/ directory"
        log_warning "Run ./scripts/setup-multi-jar.sh to initialize the project"
        exit 1
    fi

    # Create temp directory for parallel builds
    mkdir -p "$PROJECT_ROOT/temp"

    # Build header
    echo "🚀 Multi-JAR Build Pipeline"
    echo "=============================="
    echo "Project root: $PROJECT_ROOT"
    echo "JARs to build: ${jar_ids[*]}"
    echo "Mode: $([ "$COMPILE_ONLY" = true ] && echo "Compile only" || [ "$REBUILD_ONLY" = true ] && echo "Rebuild only" || echo "Full build")"
    echo "Parallel: $PARALLEL"
    echo "Verbose: $VERBOSE"
    echo ""

    # Record start time
    local start_time=$(date +%s)

    # Build JARs
    local build_success
    if $PARALLEL && [ ${#jar_ids[@]} -gt 1 ]; then
        build_jars_parallel "${jar_ids[@]}"
        build_success=$?
    else
        build_jars_sequential "${jar_ids[@]}"
        build_success=$?
    fi

    # Calculate build time
    local end_time=$(date +%s)
    local build_time=$((end_time - start_time))

    # Summary
    echo ""
    echo "📊 Build Summary"
    echo "================"
    echo "JARs processed: ${#jar_ids[@]}"
    echo "Build time: ${build_time}s"
    echo "Status: $([ $build_success -eq 0 ] && echo "✅ SUCCESS" || echo "❌ FAILED")"

    if [ $build_success -eq 0 ]; then
        echo ""
        echo "📁 Output files:"
        for jar_id in "${jar_ids[@]}"; do
            case "$jar_id" in
                lta)
                    output_jar="$PROJECT_ROOT/output/com.agfa.ris.client.lta_3090.0.13.release_20250728_rebuilt.jar"
                    ;;
                added)
                    output_jar="$PROJECT_ROOT/output/com.agfa.ris.client.studylist.lta.added_3090.0.13.release_20250728_rebuilt.jar"
                    ;;
            esac

            if [ -f "$output_jar" ]; then
                echo "   ✅ $(basename "$output_jar") ($(ls -lh "$output_jar" | awk '{print $5}'))"
            fi
        done
    fi

    exit $build_success
}

# Change to project root
cd "$PROJECT_ROOT"

# Run main function
main "$@"