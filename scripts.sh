# Extract a sample of a raster

## East Coast
gdalwarp -s_srs EPSG:4326 -t_srs EPSG:4326 -tr 0.0166666666667 0.0166666666667 -dstnodata "-32768" -q -cutline /Users/B/Documents/Projects/DrainOcean/Samples/EastCoast.gpkg -cl EastCoast -crop_to_cutline -of GTiff /Users/B/Documents/Projects/DrainOcean/ETOP/ETOPO1_Bed_c_geotiff.tif /Users/B/Documents/Projects/DrainOcean/Samples/ETOP_EastCoast.tif

## China
gdalwarp -s_srs EPSG:4326 -t_srs EPSG:4326 -tr 0.0166666666667 0.0166666666667 -dstnodata "-32768" -q -cutline /Users/B/Documents/Projects/DrainOcean/China.gpkg -cl China -crop_to_cutline -of GTiff /Users/B/Documents/Projects/DrainOcean/ETOP/ETOPO1_Bed_c_geotiff.tif /Users/B/Documents/Projects/DrainOcean/Samples/ETOP_China.tif

## Earth
gdalwarp -s_srs EPSG:4326 -t_srs EPSG:4326 -tr 0.0166666666667 0.0166666666667 -dstnodata "-32768" -q -of GTiff /Users/B/Documents/Projects/DrainOcean/ETOP/ETOPO1_Bed_c_geotiff.tif /Users/B/Documents/Projects/DrainOcean/ETOP_Cleaned.tif

# Turn a vector into a raster
gdal_rasterize -burn 2 -a_nodata "-32768" -tr 0.0166666666667 0.0166666666667 -l OceansLakes /Users/B/Documents/Projects/DrainOcean/OceansLakes.gpkg /Users/B/Documents/Projects/DrainOcean/OceansLakes.tif

# Compress
gdal_translate -co compress=LZW -co NUM_THREADS=ALL_CPUS /Users/B/Documents/Projects/DrainOcean/OceansLakes.tif /Users/B/Documents/Projects/DrainOcean/OceansLakesCompressed.tif


Usage: gdalwarp [--help-general] [--formats]
    [-s_srs srs_def] [-t_srs srs_def] [-to "NAME=VALUE"]
    [-order n | -tps | -rpc | -geoloc] [-et err_threshold]
    [-refine_gcps tolerance [minimum_gcps]]
    [-te xmin ymin xmax ymax] [-tr xres yres] [-tap] [-ts width height]
    [-ovr level|AUTO|AUTO-n|NONE] [-wo "NAME=VALUE"] [-ot Byte/Int16/...] [-wt Byte/Int16]
    [-srcnodata "value [value...]"] [-dstnodata "value [value...]"] -dstalpha
    [-r resampling_method] [-wm memory_in_mb] [-multi] [-q]
    [-cutline datasource] [-cl layer] [-cwhere expression]
    [-csql statement] [-cblend dist_in_pixels] [-crop_to_cutline]
    [-of format] [-co "NAME=VALUE"]* [-overwrite]
    [-nomd] [-cvmd meta_conflict_value] [-setci] [-oo NAME=VALUE]*
    [-doo NAME=VALUE]*
    srcfile* dstfile
