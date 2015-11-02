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
package org.exist.couchbase.xquery.document;

import com.couchbase.client.java.CouchbaseCluster;
import com.couchbase.client.java.document.JsonDocument;
import com.couchbase.client.java.document.json.JsonObject;
import org.exist.couchbase.shared.Constants;
import org.exist.couchbase.shared.CouchbaseClusterManager;
import org.exist.couchbase.shared.GenericExceptionHandler;
import org.exist.couchbase.shared.JsonToMap;
import org.exist.couchbase.shared.MapToJson;
import org.exist.couchbase.xquery.CouchbaseModule;
import org.exist.dom.QName;
import org.exist.xquery.BasicFunction;
import org.exist.xquery.Cardinality;
import org.exist.xquery.FunctionSignature;
import org.exist.xquery.XPathException;
import org.exist.xquery.XQueryContext;
import org.exist.xquery.value.FunctionParameterSequenceType;
import org.exist.xquery.value.FunctionReturnSequenceType;
import org.exist.xquery.value.Sequence;
import org.exist.xquery.value.SequenceType;
import org.exist.xquery.value.Type;

/**
 *  Upsert document into bucket
 *
 * @author Dannes Wessels
 */
public class Upsert extends BasicFunction {
    
    private static final String UPSERT = "upsert";
    private static final String INSERT = "insert";
    

    public final static FunctionSignature signatures[] = {
        new FunctionSignature(
            new QName(UPSERT, CouchbaseModule.NAMESPACE_URI, CouchbaseModule.PREFIX),
            "Upsert document into database",
            new SequenceType[]{
                new FunctionParameterSequenceType("clusterId", Type.STRING, Cardinality.ONE, "Couchbase clusterId"),
                new FunctionParameterSequenceType("bucket", Type.STRING, Cardinality.ZERO_OR_ONE, "Name of bucket, empty sequence for default bucket"),
                new FunctionParameterSequenceType("documentName", Type.STRING, Cardinality.ONE, "Name of document"),
                new FunctionParameterSequenceType("payload", Type.ITEM, Cardinality.ONE, "Json document content"),
                
            },
            new FunctionReturnSequenceType(Type.MAP, Cardinality.EXACTLY_ONE, "The new document.")
        ),
        
        new FunctionSignature(
            new QName(INSERT, CouchbaseModule.NAMESPACE_URI, CouchbaseModule.PREFIX),
            "Insert document into database",
            new SequenceType[]{
                new FunctionParameterSequenceType("clusterId", Type.STRING, Cardinality.ONE, "Couchbase clusterId"),
                new FunctionParameterSequenceType("bucket", Type.STRING, Cardinality.ZERO_OR_ONE, "Name of bucket, empty sequence for default bucket"),
                new FunctionParameterSequenceType("documentName", Type.STRING, Cardinality.ONE, "Name of document"),
                new FunctionParameterSequenceType("payload", Type.ITEM, Cardinality.ONE, "Json document content"),
                
            },
            new FunctionReturnSequenceType(Type.MAP, Cardinality.EXACTLY_ONE, "The new document.")
        ),
    };

    public Upsert(XQueryContext context, FunctionSignature signature) {
        super(context, signature);
    }

    @Override
    public Sequence eval(Sequence[] args, Sequence contextSequence) throws XPathException {

        final CouchbaseClusterManager cmm = CouchbaseClusterManager.getInstance();

        // Get connection details
        String clusterId = args[0].itemAt(0).getStringValue();

        // Get reference to cluster
        CouchbaseCluster cluster = cmm.validate(clusterId);

        // Retrieve other parameters             
        String bucketName = (args[1].isEmpty()) ? Constants.DEFAULT_BUCKET : args[1].itemAt(0).getStringValue();
        String bucketPassword = cmm.getBucketPassword(clusterId);
        
        String docName = args[2].itemAt(0).getStringValue();
        
           
        try {
            // Prepare input
            JsonObject jsonObject = (JsonObject) MapToJson.convert(args[3]);
            JsonDocument jsonDocument = JsonDocument.create(docName, jsonObject);
            
            // Perform action
            JsonDocument result = isCalledAs(UPSERT) 
                    ? cluster.openBucket(bucketName, bucketPassword).upsert(jsonDocument) 
                    : cluster.openBucket(bucketName, bucketPassword).insert(jsonDocument);
            
            // Return results
            return JsonToMap.convert(result.content(), context);
            
        } catch (Throwable ex){
            return GenericExceptionHandler.handleException(this, ex);           
        }
        
    }
    
}
