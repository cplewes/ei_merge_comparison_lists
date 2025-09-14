# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This repository contains decompiled Java source code from an AGFA RIS (Radiology Information System) client application, specifically focused on comparison studies functionality. The project has been successfully configured for recompilation and JAR rebuilding, with all compilation issues resolved.

## Development Commands

### Compilation
```bash
# Compile all Java source files
./compile.sh

# Create stub JAR with only working classes
./create-stub-jar.sh

# Analyze decompilation issues
./fix-decompilation-issues.sh
```

### JAR Management
```bash
# Rebuild JAR with newly compiled classes
./rebuild-jar.sh

# Original JAR: com.agfa.ris.client.lta_3090.0.13.release_20250728.jar
# Output JAR: com.agfa.ris.client.lta_3090.0.13.release_20250728_rebuilt.jar
```

### Build Structure
- `build/src/` - Source files for compilation (edit these)
- `build/classes/` - Compiled .class files output
- `libs/` - 756 JAR dependencies for classpath
- `classpath.txt` - Generated classpath string

## Architecture

### Key Components
- **ComparisonStudiesDock** (`com.agfa.ris.client.lta.textarea.docks.dockables.ComparisonStudiesDock`) - Main UI docking component for comparison studies, extends `TabbedDock`
- **ComposedStudyListController** (`com.agfa.ris.client.lta.textarea.studylist.ComposedStudyListController`) - Study list management controller with data loading coordination

### Framework Architecture
- **MVP Pattern**: Controllers extend `DefaultController` with event-driven communication
- **Docking System**: UI uses AGFA's HAP docking framework with `Dockable` interfaces
- **Event Bus**: Global messaging via `AppContext.getCurrentContext().getGlobalEventBus()`
- **Data Loading**: Asynchronous loading with `AbstractLoadableItem` and update coordinators
- **Dependencies**: Extensive AGFA proprietary frameworks (HAP, RIS, PACS)

## Compilation Status

### ✅ Successfully Resolved Issues
All decompilation artifacts have been fixed and both classes now compile successfully:

**ComparisonStudiesDock.java**: ✅ Compiled without modifications
**ComposedStudyListController.java**: ✅ Fixed 16 compilation errors with zero functional impact

### Types of Decompilation Artifacts Fixed
1. **Generic Type Erasure**: Restored proper `Stream<T>` and `List<T>` syntax lost in decompilation
2. **Phantom Method Calls**: Removed non-existent `this.end()` calls from anonymous transaction classes
3. **Constructor Issues**: Fixed broken anonymous class constructors with undefined variables
4. **Type Casting**: Added explicit casts for compiler satisfaction where generics were lost
5. **Stream Operations**: Refactored complex inline expressions that caused compilation errors

### Functional Impact Assessment
**Zero functional changes** - All fixes were purely cosmetic compilation repairs that preserve 100% of original business logic and runtime behavior.

## Troubleshooting Future Decompilation Issues

Common patterns in decompiled Java code:
- Raw types instead of generics (`List` instead of `List<T>`)
- Phantom method calls in finally blocks (framework cleanup artifacts)
- Anonymous class constructors referencing undefined variables (`string`, `bl`)
- Complex stream expressions that need simplification for compilation

## Critical OSGi Bundle Considerations

### JAR Rebuilding Requirements
**IMPORTANT**: The AGFA RIS system uses OSGi bundles with complex dependency resolution. When rebuilding JARs:

1. **Preserve OSGi Metadata**: Always use `jar -cfm` with original MANIFEST.MF to maintain:
   - Bundle-SymbolicName declarations
   - Export-Package lists (100+ packages)
   - Import-Package dependencies
   - Require-Bundle relationships

2. **Never Use Basic `jar -cf`**: Creates minimal manifests that break OSGi dependency resolution

3. **Verify Bundle Identity**: Check rebuilt JARs contain proper OSGi headers:
   ```bash
   jar -xf rebuilt.jar META-INF/MANIFEST.MF
   grep "Bundle-SymbolicName.*com.agfa.ris.client.lta" META-INF/MANIFEST.MF
   ```

### Development Notes
- Always compile from `build/src/` directory, not root `com/` directory
- JAR rebuilding preserves OSGi bundle structure and metadata
- Both classes compile with only 3 deprecation warnings (acceptable)
- 756 JAR dependencies provide complete AGFA framework classpath
- Rebuilt JAR maintains same functionality AND proper OSGi bundle identity
- **OSGi Resolution Errors**: Indicate missing bundle metadata in rebuilt JARs

## Blended Comparison List Feature

### Feature Description
Implemented a "blended comparison list" feature that automatically shows Added studies in the main Comparison list without requiring user clicks, providing a seamless zero-click UX.

### Implementation Details

**Files Modified:**
- `ComparisonStudiesDock.java`: Added auto-triggering logic in `triggerSearch()` method
- `ComposedStudyListController.java`: Enhanced `addAddedComparison()` method with blending capability

**Key Changes:**
1. **Auto-trigger Added search**: When Comparison tab is selected, automatically triggers the same first-time Added search
2. **Blend Added into Comparison**: Each Added study is automatically added to the main Comparison list without filtering
3. **Preserve Added tab functionality**: Added studies remain visible in their dedicated tab

### Critical Filtering Issue Resolution

**Problem Identified:** Initial blended implementation only showed 25/41 Added studies due to filtering.

**Root Cause Analysis:**
- `mergeComparisons()` applies `filterByProcedureStatus()` which filters out external studies from archives like "IDC North"
- Filter excludes studies that are NOT current/associated AND cancelled AND have no reports AND have no images
- External archive studies are labeled "External Study" vs "Comparison Study" and often meet exclusion criteria
- `addAddedComparison()` adds studies directly without filtering (shows all 41 studies)

**Solution Implemented:**
- Replaced `mergeComparisons()` call with direct addition logic in `addAddedComparison()`
- Studies are added directly to `additionalComparisons` without `filterByProcedureStatus()` filtering
- Maintains deduplication logic and proper model updates
- Preserves AeCode handling for LOCAL vs external studies

**Code Pattern:**
```java
// Direct addition to avoid filtering that excludes external studies
if (!this.containsStudy(this.additionalComparisons, requestedProcedure) &&
    !this.containsStudy(this.model.getComparisonStudies(), requestedProcedure)) {
    this.additionalComparisons.add(requestedProcedure);
    // Update main comparison model and ReportingContext
}
```

**Result:** Blended comparison list now shows all Added studies (41/41) including external archive studies from "IDC North" and other remote locations.