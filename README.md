
## Run


### Simulate Draining

sbt "run --job simDrain --elev_raster /Users/B/Documents/Projects/DrainOcean/Samples/ETOP_EastCoast2.tif --x 1900 --y 1250 --elev 0"


### Combine Water

sbt "run --job combineWater --water_a /Users/B/Documents/Projects/DrainOcean/OceanLakes/OceansLakes.tif --water_b /Users/B/Documents/Projects/DrainOcean/ETOP/ETOP_CleanedProcessed_00070.tif --elev 70"
