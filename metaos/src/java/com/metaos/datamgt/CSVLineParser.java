/*
 * Copyright 2011 - 2012
 * All rights reserved. License and terms according to LICENSE.txt file.
 * The LICENSE.txt file and this header must be included or referenced 
 * in each piece of code derived from this project.
 */
package com.metaos.datamgt;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;
import java.util.logging.Logger;

/**
 * Line processor for CSV files with only one date and symbol per line.
 *
 * <b>Not thread safe</b>
 */
public class CSVLineParser implements LineParser {
    private final Format[] formatters;
    private final Field[] fieldNames;
    private final ParsePosition[] parsePositions;
    private final int symbolIndex, dateIndexes[];
    private final List<CacheListener> cacheListeners;
    private final List<Filter> pricesFilters;

    private String parsedLine;
    private ParseResult parsedData;
    private boolean parsingResult;
    
    /**
     * Creates a parser for CSV files.
     * @param formatters list of formatters to translate string into numbers,
     *      strings or dates.
     * @param fieldNames name of fields to notify listeners, null for fields 
     * that will be ignored.
     * @param symbolIndex index of the previous list of formatters for the
     *      symbol name (should be null in the previous list of fieldNames).
     * @param dateIndex index of the previous list of formatters for the
     *      date of the line (should be null in the previous list of 
     *      fieldNames).
     */
    public CSVLineParser(final Format formatters[],
            final Field[] fieldNames, final int symbolIndex, 
            final int dateIndexes[]) {
        assert (fieldNames.length == formatters.length);
        assert (symbolIndex < fieldNames.length);

        this.cacheListeners = new ArrayList<CacheListener>();
        this.pricesFilters = new ArrayList<Filter>();
        this.dateIndexes = dateIndexes;
        this.symbolIndex = symbolIndex;
        this.fieldNames = new Field[fieldNames.length];
        this.formatters = new Format[formatters.length];
        this.parsePositions = new ParsePosition[formatters.length];
        for(int i=0; i<parsePositions.length; i++) {
            this.formatters[i] = formatters[i];
            this.fieldNames[i] = fieldNames[i];
            this.parsePositions[i] = new ParsePosition(0);
        }
        this.parsedData = new ParseResult();
    }


    public boolean isValid(final String line) {
        if( ! line.equals(this.parsedLine) ) {
            _parseLine(line);
        }
        return this.parsedData.getSymbol(0) != null 
                && this.parsedData.getCalendar() != null
                && this.parsingResult;
    }


    public ParseResult parse(final String line) {
        if( ! line.equals(this.parsedLine) ) {
            _parseLine(line);
        }

/*
        for(final CacheListener listener : this.cacheListeners) {
            for(final Map.Entry<Field, Double> entry
                    : this.parsedValues.entrySet()) {
                
                entry.getKey().notify(listener, this.parsedDatathis.parsedData.getCalendar(),
                        this.parsedData.getSymbol(), entry.getValue());
            }
        }
*/
        return this.parsedData;
    }


    public void addFilter(final Filter filter) {
        this.pricesFilters.add(filter);
    }



    public void addCacheListener(final CacheListener listener) {
        this.cacheListeners.add(listener);
    }

/*
    public void concludeLineSet() {
        if(this.parsedData.getSymbol()==null || 
                this.parsedData.getCalendar()==null) return;

        boolean pricesAreValid = true;
        for(final Filter filter : this.pricesFilters) {
            if( ! filter.filter(this.parsedData.getCalendar(), 
                    this.parsedData.getSymbol(), this.parsedValues)) {
                pricesAreValid = false;
                break;
            }
        }

        if(pricesAreValid) {
            for(final CacheListener listener : this.cacheListeners) {
                for(final Map.Entry<Field, Double> entry
                        : this.parsedValues.entrySet()) {
                    entry.getKey().notify(listener, 
                            this.parsedData.getCalendar(),
                            this.parsedData.getSymbol(), entry.getValue());
                }
            }

            final List<String> symbolArray = Arrays.asList(
                    this.parsedData.getSymbol());
            for(final Listener listener : this.pricesListeners) {
                listener.update(symbolArray, this.parsedData.getCalendar());
            }
        }

        this.parsedData.getSymbol() = null;
        this.parsedData.getCalendar() = null;
    }
*/


    public String getSymbol(final String line, final int index) {
        if( ! line.equals(this.parsedLine) ) {
            _parseLine(line);
        }
        return this.parsedData.getSymbol(index);
    }


    public Calendar getTimestamp(final String line) {
        if( ! line.equals(this.parsedLine) ) {
            _parseLine(line);
        }
        return this.parsedData.getCalendar();
    }


    //
    // Private stuff ----------------------------------------------
    //

    /**
     * Modifies internal values trying to parse given line.
     */
    private void _parseLine(final String line) {
        this.parsedLine = line;
        this.parsedData.reset();
        this.parsingResult = false;

        final String parts[] = line.split(",");
        boolean allLineProcessed = true;
        for(int i=0; i<parts.length; i++) {
            if(this.formatters[i] != null) {
                try {
                    this.parsePositions[i].setIndex(0);
                    final Object obj = this.formatters[i]
                            .parseObject(parts[i], this.parsePositions[i]);
                    if(obj instanceof Object[]) {
                        if(i==this.symbolIndex) {
                            this.parsedData.addSymbol((String) 
                                    ((Object[])obj)[0]);
                        }
                    } else if(obj instanceof Double) {
                        this.parsedData.putValue(
                                this.fieldNames[i], (Double) obj);
                    } else if(obj instanceof Date) {
                        for(int j=0; j<dateIndexes.length; j++) {
                            if(i==this.dateIndexes[j]) {
                                if(this.parsedData.getCalendar()==null) {
                                    this.parsedData.newCalendar();
                                    this.parsedData.getCalendar()
                                            .setTimeInMillis(((Date) obj)
                                                .getTime());
                                } else {
                                    this.parsedData.getCalendar()
                                        .setTimeInMillis(this.parsedData
                                            .getCalendar().getTimeInMillis() 
                                            + ((Date) obj).getTime());
                                }
                                break;
                            }
                        }
                    } else {
                        // Unknown type
                        allLineProcessed = false;
                    }
                } catch(Exception e) {
                    allLineProcessed = false;
                }
            }
        }

        this.parsingResult = allLineProcessed;
    }
}