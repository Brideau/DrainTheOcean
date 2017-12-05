# Extract a sample of a raster

## East Coast
gdalwarp -s_srs EPSG:4326 -t_srs EPSG:4326 -tr 0.0166666666667 0.0166666666667 -dstnodata "-32768" -q -cutline /Users/B/Documents/Projects/DrainOcean/Samples/EastCoast.gpkg -cl EastCoast -crop_to_cutline -of GTiff /Users/B/Documents/Projects/DrainOcean/ETOP/ETOPO1_Bed_c_geotiff.tif /Users/B/Documents/Projects/DrainOcean/Samples/ETOP_EastCoast.tif

## China
gdalwarp -s_srs EPSG:4326 -t_srs EPSG:4326 -tr 0.0166666666667 0.0166666666667 -dstnodata "-32768" -q -cutline /Users/B/Documents/Projects/DrainOcean/China.gpkg -cl China -crop_to_cutline -of GTiff /Users/B/Documents/Projects/DrainOcean/ETOP/ETOPO1_Bed_c_geotiff.tif /Users/B/Documents/Projects/DrainOcean/Samples/ETOP_China.tif

## Earth
gdalwarp -s_srs EPSG:4326 -t_srs EPSG:4326 -tr 0.0166666666667 0.0166666666667 -dstnodata "-32768" -q -of GTiff /Users/B/Documents/Projects/DrainOcean/ETOP/ETOPO1_Bed_c_geotiff.tif /Users/B/Documents/Projects/DrainOcean/ETOP_Cleaned.tif

# Turn a vector into a raster
gdal_rasterize -burn 2 -a_nodata "-32768" -co COMPRESS=LZW -co NUM_THREADS=ALL_CPUS -te -180 -90 180 90 -ts 21600 10800 -l OceansLakes /Users/B/Documents/Projects/DrainOcean/OceansLakes.gpkg /Users/B/Documents/Projects/DrainOcean/OceanLakes/OceansLakes.tif

# Compress
gdal_translate -co compress=LZW -co NUM_THREADS=ALL_CPUS /Users/B/Documents/Projects/DrainOcean/OceansLakes.tif /Users/B/Documents/Projects/DrainOcean/OceansLakesCompressed.tif
