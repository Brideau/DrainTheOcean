
## Drain the Oceans

A simulation of the ocean draining from the Marianas Trench (or any other location) using a scanline flood fill algorithm adapted for geospatial use.

![Drain the Ocean](https://i.imgur.com/B0iaXzl.jpg)


**TODO: Clean up all of the below**

### Generate Flood Fill Masks

sbt "run --job generateFloodFillMasks --x 19353 --y 4716 --output_path [output_directory]"

### Combine Water

sbt "run --job combineWater --water_a [base_directory]/OceanLakes/OceansLakes.tif --water_b [base_directory]/ETOP_CleanedProcessed_00070.tif --elev 70 --output_path [output_directory]"

### Generate Base Layer PNG

sbt "run --job generatePngs --base_raster [base_directory]/ETOP_Cleaned.tif --output_path [output_directory]"

### Generate All Water PNGs
sbt "run --job generatePngs --output_path [output_directory]"

### Process All PNGs
sbt "run --job processPngs --output_path [output_directory]"

### Add Text to PNGs
sbt "run --job addText --output_path [output_directory]"

### Find Min Location
sbt "run --job getMinLoc --elev_raster [base_directory]/ETOP_Cleaned.tif"