/*
 *  eXist Open Source Native XML Database
 *  Copyright (C) 2015 The eXist Project
 *  http://exist-db.org
 *
 *  This program is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public License
 *  as published by the Free Software Foundation; either version 2
 *  of the License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 */
package org.exist.couchbase.xquery.bucket;

import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.view.ViewQuery;
import com.couchbase.client.java.view.ViewResult;
import com.couchbase.client.java.view.ViewRow;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.apache.commons.lang3.StringUtils;
import org.exist.couchbase.shared.Constants;
import org.exist.couchbase.shared.ConversionTools;
import org.exist.couchbase.shared.CouchbaseClusterManager;
import org.exist.couchbase.xquery.CouchbaseModule;
import org.exist.dom.QName;
import org.exist.xquery.BasicFunction;
import org.exist.xquery.Cardinality;
import org.exist.xquery.FunctionSignature;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.functions.map.AbstractMapType;
import org.exist.xquery.value.FunctionParameterSequenceType;
import org.exist.xquery.value.FunctionReturnSequenceType;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.SequenceType;
import org.exist.xquery.value.StringValue;
import org.exist.xquery.value.Type;
import org.exist.xquery.value.ValueSequence;

/**
 * Retrieve document
 *
 * @author Dannes Wessels
 */
public class Query extends BasicFunction {
    
    public final static FunctionSignature signatures[] = {
        new FunctionSignature(
        new QName("query", CouchbaseModule.NAMESPACE_URI, CouchbaseModule.PREFIX),
        "Query a view with the default view timeout.",
        new SequenceType[]{
            new FunctionParameterSequenceType("clusterId", Type.STRING, Cardinality.ONE, "Couchbase clusterId"),
            new FunctionParameterSequenceType("bucket", Type.STRING, Cardinality.ZERO_OR_ONE, "Name of bucket, empty sequence for default bucket"),
            new FunctionParameterSequenceType("design", Type.STRING, Cardinality.ONE, "Name of design document"),
            new FunctionParameterSequenceType("view", Type.STRING, Cardinality.ONE, "Name of view"),
            new FunctionParameterSequenceType("parameters", Type.MAP, Cardinality.ZERO_OR_ONE, "Query parameters")
        
        },
        new FunctionReturnSequenceType(Type.STRING, Cardinality.ZERO_OR_MORE, "Results of query, JSON formatted.")
        ),};
    
    public Query(XQueryContext context, FunctionSignature signature) {
        super(context, signature);
    }
    
    @Override
    public Sequence eval(Sequence[] args, Sequence contextSequence) throws XPathException {

        // User must either be DBA or in the c group
        if (!context.getSubject().hasDbaRole() && !context.getSubject().hasGroup(Constants.COUCHBASE_GROUP)) {
            String txt = String.format("Permission denied, user '%s' must be a DBA or be in group '%s'",
                    context.getSubject().getName(), Constants.COUCHBASE_GROUP);
            LOG.error(txt);
            throw new XPathException(this, txt);
        }

        // Get connection details
        String clusterId = args[0].itemAt(0).getStringValue();
        CouchbaseClusterManager.getInstance().validate(clusterId);

        // Retrieve other parameters             
        String bucketName = (args[1].isEmpty())
                ? null
                : args[1].itemAt(0).getStringValue();
        
        String design = args[2].itemAt(0).getStringValue();
        String view = args[3].itemAt(0).getStringValue();
        
        Map<String, Object> parameters = (args[4].isEmpty())
                ? new HashMap<String, Object>()
                : ConversionTools.convert((AbstractMapType) args[4].itemAt(0));

        // Retrieve access to cluster
        CouchbaseCluster cluster = CouchbaseClusterManager.getInstance().get(clusterId);
        
        try {
            // Prepare query
            ViewQuery viewQuery = ViewQuery.from(design, view);
            
            // Set additional parameters
            viewQuery = parseParameters(viewQuery, parameters);

            // Perform action
            ViewResult result = StringUtils.isBlank(bucketName)
                    ? cluster.openBucket().query(viewQuery)
                    : cluster.openBucket(bucketName).query(viewQuery);

            // Return results
            ValueSequence retVal = new ValueSequence();
            
            for (ViewRow row : result) {
                retVal.add(new StringValue(ConversionTools.convert(row.document().content())));
            }
            
            return retVal;
            
        } catch (Exception ex) {
            // TODO detailed error handling
            LOG.error(ex.getMessage(), ex);
            throw new XPathException(this, ex.getMessage(), ex);
        }
        
    }
    
    private ViewQuery parseParameters(ViewQuery viewQuery, Map<String, Object> parameters) throws XPathException {
        
        for (Entry<String, Object> entry : parameters.entrySet()) {
            
            String key = entry.getKey();
            Object value = entry.getValue();
            
            switch (key) {
                case "descending":
                    viewQuery.descending(getBooleanValue(key, value, false));
                    break;
                case "group":
                    viewQuery.group(getBooleanValue(key, value, false));
                    break;
                case "reduce":
                    viewQuery.reduce(getBooleanValue(key, value, false));
                    break;
                case "development":
                    viewQuery.development(getBooleanValue(key, value, false));
                    break;
                case "inclusive_end":
                    viewQuery.inclusiveEnd(getBooleanValue(key, value, false));
                    break;
                case "debug":
                    viewQuery.debug(getBooleanValue(key, value, false));
                    break;
                case "limit":
                    viewQuery.limit(getIntValue(key, value, 0));
                    break;
                case "group_level":
                    viewQuery.groupLevel(getIntValue(key, value, 0));
                    break;
                case "skip":
                    viewQuery.skip(getIntValue(key, value, 0));
                    break;
                case "key":
                    viewQuery.key(value.toString());
                    break;
                case "startkey":
                    viewQuery.startKey(value.toString());
                    break;
                case "endkey":
                    viewQuery.endKey(value.toString());
                    break;
                case "startkey_docid":
                    viewQuery.startKeyDocId(value.toString());
                    break;
                case "endkey_docid":
                    viewQuery.endKeyDocId(value.toString());
                    break;
                default:
                    throw new XPathException(key + " is not a valid parameter.");
            }
            
        }
        
        return viewQuery;
        
    }
    
    private boolean getBooleanValue(String key, Object obj, boolean def) throws XPathException {
        
        if (obj == null) {
            return def;
        }
        
        if (!(obj instanceof Boolean)) {
            throw new XPathException(String.format("Map item '%s' is not a Boolean value (%s)", key, obj.toString()));
        }
        
        return (Boolean) obj;
    }
    
    private int getIntValue(String key, Object obj, int def) throws XPathException {
        
        if (obj == null) {
            return def;
        }
        
        if (!(obj instanceof Integer)) {
            throw new XPathException(String.format("Map item '%s' is not a Integer value (%s)", key, obj.toString()));
        }
        
        return (Integer) obj;
    }
}
