
## Run


### Simulate Draining

sbt "run --job simDrain --elev_raster /Users/B/Documents/Projects/DrainOcean/ETOP/ETOP_Cleaned.tif --water_raster /Users/B/Documents/Projects/DrainOcean/Output/MergedWater/OceansLakesProcessed_000000.tif --x 19400 --y 4300 --elev 0 --output_path /Users/B/Documents/Projects/DrainOcean/Output"


### Combine Water

sbt "run --job combineWater --water_a /Users/B/Documents/Projects/DrainOcean/OceanLakes/OceansLakes.tif --water_b /Users/B/Documents/Projects/DrainOcean/ETOP/ETOP_CleanedProcessed_00070.tif --elev 70 --output_path /Users/B/Documents/Projects/DrainOcean/Output"

### Generate Base Layer PNG

sbt "run --job generatePngs --base_raster /Users/B/Documents/Projects/DrainOcean/ETOP/ETOP_Cleaned.tif --output_path /Users/B/Documents/Projects/DrainOcean/Output"

### Generate All Water PNGs
sbt "run --job generatePngs --output_path /Users/B/Documents/Projects/DrainOcean/Output"

### Process All PNGs
sbt "run --job processPngs --output_path /Users/B/Documents/Projects/DrainOcean/Output"

### Add Text to PNGs
sbt "run --job addText --output_path /Users/B/Documents/Projects/DrainOcean/Output"