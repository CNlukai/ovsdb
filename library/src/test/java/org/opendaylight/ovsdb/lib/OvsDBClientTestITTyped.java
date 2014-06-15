/*
 * Copyright (C) 2014 EBay Software Foundation
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Authors : Ashwin Raveendran, Madhu Venugopal
 */
package org.opendaylight.ovsdb.lib;

import static org.opendaylight.ovsdb.lib.operations.Operations.op;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opendaylight.ovsdb.lib.message.OvsdbRPC;
import org.opendaylight.ovsdb.lib.message.UpdateNotification;
import org.opendaylight.ovsdb.lib.notation.Mutator;
import org.opendaylight.ovsdb.lib.notation.UUID;
import org.opendaylight.ovsdb.lib.operations.OperationResult;
import org.opendaylight.ovsdb.lib.operations.TransactionBuilder;
import org.opendaylight.ovsdb.lib.schema.ColumnSchema;
import org.opendaylight.ovsdb.lib.schema.DatabaseSchema;
import org.opendaylight.ovsdb.lib.schema.GenericTableSchema;
import org.opendaylight.ovsdb.lib.schema.TableSchema;
import org.opendaylight.ovsdb.lib.schema.temp.Reference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.ListenableFuture;

public class OvsDBClientTestITTyped extends OvsdbTestBase {

    Logger logger = LoggerFactory.getLogger(OvsDBClientTestITTyped.class);
    OvsDBClientImpl ovs;
    DatabaseSchema dbSchema = null;
    static String testBridgeName = "br-test";
    static UUID testBridgeUuid = null;

    public static class Bridge extends TableSchema<Bridge> {

        public Bridge (TableSchema<Bridge> tableSchema) {
            super("Bridge", tableSchema.getColumnSchemas());
        }

        public Bridge(String name, Map<String, ColumnSchema> columns) {
            super(name, columns);
        }

        public ColumnSchema<Bridge, String> name() {
            return column("name", String.class);
        }

        public ColumnSchema<Bridge, Integer> floodVlans() {
            return column("flood_vlans", Integer.class);
        }

        public ColumnSchema<Bridge, Map> status() {
            return column("status", Map.class);
        }

        public ColumnSchema<Bridge, Reference> netflow() {
            return column("netflow", Reference.class);
        }
    }


    @Test
    public void testTypedBridgeCreate() throws IOException, InterruptedException, ExecutionException {
        Bridge bridge = dbSchema.table("Bridge", Bridge.class);
        GenericTableSchema ovsTable = dbSchema.table("Open_vSwitch", GenericTableSchema.class);
        ColumnSchema<GenericTableSchema, Set<UUID>> bridges = ovsTable.multiValuedColumn("bridges", UUID.class);

        GenericTableSchema anytable = null;
        String namedUuid = "br_test";
        int insertOperationIndex = 0;

        TransactionBuilder transactionBuilder = ovs.transactBuilder()
                .add(op.insert(bridge)
                        .withId(namedUuid)
                        .value(bridge.name(), testBridgeName))
                .add(op.update(bridge)
                        .set(bridge.status(), Maps.newHashMap(ImmutableMap.of("key","value")))
                        .set(bridge.floodVlans(), 34)
                        .where(bridge.name().opEqual(testBridgeName))
                        .and(bridge.name().opEqual(testBridgeName)).build())
                .add(op.mutate(ovsTable)
                        .addMutation(bridges, Mutator.INSERT, Sets.newHashSet(new UUID(namedUuid))));

        ListenableFuture<List<OperationResult>> results = transactionBuilder.execute();
        List<OperationResult> operationResults = results.get();
        Assert.assertFalse(operationResults.isEmpty());
        // Check if Results matches the number of operations in transaction
        Assert.assertEquals(transactionBuilder.getOperations().size(), operationResults.size());
        System.out.println("Insert & Update operation results = " + operationResults);
        testBridgeUuid = operationResults.get(insertOperationIndex).getUuid();
    }

    public void testGetDBs() throws ExecutionException, InterruptedException {
        ListenableFuture<List<String>> databases = ovs.getDatabases();
        List<String> dbNames = databases.get();
        Assert.assertNotNull(dbNames);
        boolean hasOpenVswitchSchema = false;
        for(String dbName : dbNames) {
           if (dbName.equals(OPEN_VSWITCH_SCHEMA)) {
                hasOpenVswitchSchema = true;
                break;
           }
        }
        Assert.assertTrue(OPEN_VSWITCH_SCHEMA+" schema is not supported by the switch", hasOpenVswitchSchema);
    }

    @Before
    public  void setUp() throws IOException, ExecutionException, InterruptedException {
        if (ovs != null) {
            return;
        }
        OvsdbRPC rpc = getTestConnection();
        if (rpc == null) {
            System.out.println("Unable to Establish Test Connection");
        }
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        ovs = new OvsDBClientImpl(rpc, executorService);
        testGetDBs();
        dbSchema = ovs.getSchema(OPEN_VSWITCH_SCHEMA, true).get();
    }

    @After
    public void tearDown() throws InterruptedException, ExecutionException {
        TableSchema<GenericTableSchema> bridge = dbSchema.table("Bridge", GenericTableSchema.class);
        ColumnSchema<GenericTableSchema, String> name = bridge.column("name", String.class);
        GenericTableSchema ovsTable = dbSchema.table("Open_vSwitch", GenericTableSchema.class);
        ColumnSchema<GenericTableSchema, Set<UUID>> bridges = ovsTable.multiValuedColumn("bridges", UUID.class);

        ListenableFuture<List<OperationResult>> results = ovs.transactBuilder()
                .add(op.delete(bridge)
                        .where(name.opEqual(testBridgeName))
                        .build())
                .add(op.mutate(ovsTable)
                        .addMutation(bridges, Mutator.DELETE, Sets.newHashSet(testBridgeUuid)))
                .add(op.commit(true))
                .execute();

        List<OperationResult> operationResults = results.get();
        System.out.println("Delete operation results = " + operationResults);
    }

    @Override
    public void update(Object node, UpdateNotification upadateNotification) {
        // TODO Auto-generated method stub

    }
    @Override
    public void locked(Object node, List<String> ids) {
        // TODO Auto-generated method stub

    }
    @Override
    public void stolen(Object node, List<String> ids) {
        // TODO Auto-generated method stub

    }

}