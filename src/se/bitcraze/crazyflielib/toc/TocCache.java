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

package se.bitcraze.crazyflielib.toc;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
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
    private String mRwCachePath;
    private ObjectMapper mMapper = new ObjectMapper(); // can reuse, share globally

    public TocCache(String roCachePath, String rwCachePath) {
        addCacheFiles(roCachePath);
        addCacheFiles(rwCachePath);
        this.mRwCachePath = rwCachePath;
    }

    private void addCacheFiles(String cachePath) {
        if (cachePath != null) {
            File cacheDir = new File(cachePath);
            if(cacheDir.exists()) {
                for(File jsonFile : cacheDir.listFiles(jsonFilter)) {
                    this.mCacheFiles.add(jsonFile);
                }
            } else {
                cacheDir.mkdirs();
            }
        }
    }

    FilenameFilter jsonFilter = new FilenameFilter() {
        public boolean accept(File dir, String name) {
            return name.endsWith(".json") ? true : false;
        }
    };

    /**
     * Try to get a hit in the cache, return None otherwise
     *
     * @param crc
     */
    public Toc fetch(int crc) {
        Toc fetchedToc = null;
        String pattern = String.format("%08X.json", crc);
        File hit = null;

        mLogger.debug("Trying to find TOC cache file: " + pattern);

        for (File file : mCacheFiles) {
            if(file.getName().endsWith(pattern)) {
                hit = file;
            }
        }
        if (hit != null) {
            mLogger.debug("Found TOC cache file: " + pattern);
            try {
                fetchedToc = new Toc();
                Map<String, TocElement> readValue = mMapper.readValue(hit, new TypeReference<Map<String, TocElement>>() { });
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
    public void insert (int crc, Toc toc) {
        if (mRwCachePath != null) {
            String fileName = String.format("%s/%08X.json", mRwCachePath, crc);
            try {
                this.mMapper.enable(SerializationFeature.INDENT_OUTPUT);
                this.mMapper.writeValue(new File(fileName), toc.getTocElementMap());
                //TODO: add "__class__" : "LogTocElement",
                this.mLogger.info("Saved cache to " + fileName);
                this.mCacheFiles.add(new File(fileName));
                //TODO: file leak?
            } catch (JsonGenerationException jge) {
                mLogger.error("Could not save cache to file " + fileName + ".\n" + jge.getMessage());
            } catch (JsonMappingException jme) {
                mLogger.error("Could not save cache to file " + fileName + ".\n" + jme.getMessage());
            } catch (IOException ioe) {
                mLogger.error("Could not save cache to file " + fileName + ".\n" + ioe.getMessage());
            }
        } else {
            mLogger.error("Could not save cache, no writable directory");
        }
    }

}
