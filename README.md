
## Run


### Simulate Draining

sbt "run --job simDrain --elev_raster /Users/B/Documents/Projects/DrainOcean/ETOP/ETOP_Cleaned.tif --water_raster /Users/B/Documents/Projects/DrainOcean/Output/MergedWater/OceansLakesProcessed_000000.tif --x 19353 --y 4716 --elev 0 --output_path /Users/B/Documents/Projects/DrainOcean/Output"

sbt "run --job generateFloodFillMasks --x 19353 --y 4716 --output_path /Users/B/Documents/Projects/DrainOcean/Output"

sbt "run --job generateFloodFillMasks --x 19353 --y 4716 --output_path /home/brideauadmin/Documents/DrainTheOceanResources/Output"

sbt "run --job simDrain --elev_raster /home/brideauadmin/Documents/DrainTheOceanResources/ETOP_Cleaned.tif --water_raster /home/brideauadmin/Documents/DrainTheOceanResources/OceansLakesProcessed_000000.tif --x 19353 --y 4716 --elev 0 --output_path /home/brideauadmin/Documents/DrainTheOceanResources/Output"

#### Test Small
sbt "run --job simDrain --testFill --elev_raster /Users/B/Documents/Projects/DrainOcean/Samples/ETOP_EastCoast2.tif --water_raster /Users/B/Documents/Projects/DrainOcean/Output/MergedWater/OceansLakesProcessed_000000.tif --x 1900 --y 1200 --elev 0 --output_path /Users/B/Documents/Projects/DrainOcean/Output"

#### Test large
sbt "run --job simDrain --testFill --elev_raster /Users/B/Documents/Projects/DrainOcean/ETOP/ETOP_Cleaned.tif --water_raster /Users/B/Documents/Projects/DrainOcean/Output/MergedWater/OceansLakesProcessed_000000.tif --x 19353 --y 4716 --elev 0 --output_path /Users/B/Documents/Projects/DrainOcean/Output"

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

### Find Min Location
sbt "run --job getMinLoc --elev_raster /Users/B/Documents/Projects/DrainOcean/ETOP/ETOP_Cleaned.tif"