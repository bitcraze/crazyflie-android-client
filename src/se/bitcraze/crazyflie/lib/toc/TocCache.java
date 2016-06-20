/**
 *    ||          ____  _ __
 * +------+      / __ )(_) /_______________ _____  ___
 * | 0xBC |     / __  / / __/ ___/ ___/ __ `/_  / / _ \
 * +------+    / /_/ / / /_/ /__/ /  / /_/ / / /_/  __/
 *  ||  ||    /_____/_/\__/\___/_/   \__,_/ /___/\___/
 *
 * Copyright (C) 2015 Bitcraze AB
 *
 * Crazyflie Nano Quadcopter Client
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *
 */

package se.bitcraze.crazyflie.lib.toc;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import se.bitcraze.crazyflie.lib.crtp.CrtpPort;
import se.bitcraze.crazyflie.lib.log.LogTocElement;
import se.bitcraze.crazyflie.lib.param.ParamTocElement;

/**
 *  Access to TOC cache. To turn off the cache functionality don't supply any directories.
 *
 *  Heavily based on toccache.py
 *
 *
 */
public class TocCache {

    final Logger mLogger = LoggerFactory.getLogger("TocCache");
    private List<File> mCacheFiles = new ArrayList<File>();
    private File mCacheDir = null;
    private static final String PARAM_CACHE_DIR = "paramCache";
    private static final String LOG_CACHE_DIR = "logCache";
    private ObjectMapper mMapper = new ObjectMapper(); // can reuse, share globally

    public TocCache(File cacheDir) {
        this.mCacheDir = cacheDir;
        //TODO: should it be possible to change the name of the dirs?
        addExistingCacheFiles(LOG_CACHE_DIR);
        addExistingCacheFiles(PARAM_CACHE_DIR);
    }

    private void addExistingCacheFiles(String cachePath) {
        if (cachePath != null) {
            //use cache dir if it's not null
            File cachePathFile = (mCacheDir != null) ? new File(mCacheDir, cachePath) : new File(cachePath);
            if(cachePathFile.exists()) {
                this.mCacheFiles.addAll(Arrays.asList(cachePathFile.listFiles(jsonFilter)));
            }
        }
    }

    FilenameFilter jsonFilter = new FilenameFilter() {
        public boolean accept(File dir, String name) {
            return name.endsWith(".json");
        }
    };

    /**
     * Try to get a hit in the cache, return None otherwise
     *
     * @param crc CRC code of the TOC
     * @param port CrtpPort of the TOC
     */
    public Toc fetch(int crc, CrtpPort port) {
        Toc fetchedToc = null;
        String pattern = String.format("%08X.json", crc);
        File hit = null;

        mLogger.debug("Trying to find existing TOC cache file: " + pattern);

        for (File file : mCacheFiles) {
            if(file.getName().endsWith(pattern)) {
                hit = file;
            }
        }
        if (hit != null) {
            mLogger.debug("Found TOC cache file: " + pattern);
            try {
                fetchedToc = new Toc();
                Map<String, TocElement> readValue;
                if (port == CrtpPort.PARAMETERS) {
                    readValue = mMapper.readValue(hit, new TypeReference<Map<String, ParamTocElement>>() { });
                } else {
                    readValue = mMapper.readValue(hit, new TypeReference<Map<String, LogTocElement>>() { });
                }
                fetchedToc.setTocElementMap(readValue);
                mLogger.debug("Number of cached elements: " + fetchedToc.getElements().size());
                //TODO: file leak?
            } catch (JsonParseException jpe) {
                mLogger.error("Error while parsing cache file " + hit.getName() + ": " + jpe.getMessage());
                return null;
            } catch (JsonMappingException jme) {
                mLogger.error("Error while parsing cache file " + hit.getName() + ": " + jme.getMessage());
                return null;
            } catch (IOException ioe) {
                mLogger.error("Error while parsing cache file " + hit.getName() + ": " + ioe.getMessage());
                return null;
            }
        }
        return fetchedToc;
    }

    /**
     * Save a new cache to file
     */
    public void insert (int crc, CrtpPort port,  Toc toc) {
        String fileName = String.format("%08X.json", crc);
        String subDir = (port == CrtpPort.PARAMETERS) ? PARAM_CACHE_DIR : LOG_CACHE_DIR;
        File cacheDir = (mCacheDir != null) ? new File(mCacheDir, subDir) : new File(subDir);
        File cacheFile = new File(cacheDir, fileName);
        try {
            if (!cacheFile.exists()) {
                cacheFile.getParentFile().mkdirs();
                cacheFile.createNewFile();
            }
            this.mMapper.enable(SerializationFeature.INDENT_OUTPUT);
            this.mMapper.writeValue(cacheFile, toc.getTocElementMap());
            //TODO: add "__class__" : "LogTocElement",
            this.mLogger.info("Saved cache to " + fileName);
            this.mCacheFiles.add(cacheFile);
            //TODO: file leak?
        } catch (JsonGenerationException jge) {
            mLogger.error("Could not save cache to file " + fileName + ".\n" + jge.getMessage());
        } catch (JsonMappingException jme) {
            mLogger.error("Could not save cache to file " + fileName + ".\n" + jme.getMessage());
        } catch (IOException ioe) {
            mLogger.error("Could not save cache to file " + fileName + ".\n" + ioe.getMessage());
        }
    }

    public void clear() {
        for (File file : mCacheFiles) {
            boolean delete = file.delete();
            if (!delete) {
                mLogger.error("Deleting cache file " + file.getAbsolutePath() + " failed.");
            }
        }
    }
}
