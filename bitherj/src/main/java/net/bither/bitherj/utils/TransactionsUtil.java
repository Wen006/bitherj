/*
 * Copyright 2014 http://Bither.net
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.bither.bitherj.utils;

import net.bither.bitherj.AbstractApp;
import net.bither.bitherj.BitherjSettings;
import net.bither.bitherj.api.BitherMytransactionsApi;
import net.bither.bitherj.api.BlockChainMytransactionsApi;
import net.bither.bitherj.core.*;
import net.bither.bitherj.api.BitherQueryAddressApi;
import net.bither.bitherj.api.BitherQueryAddressUnspentApi;
import net.bither.bitherj.api.BitherUnspentTxsApi;
import net.bither.bitherj.core.AbstractHD;
import net.bither.bitherj.core.Address;
import net.bither.bitherj.core.AddressManager;
import net.bither.bitherj.core.Block;
import net.bither.bitherj.core.BlockChain;
import net.bither.bitherj.core.HDAccount;
import net.bither.bitherj.core.HDMAddress;
import net.bither.bitherj.core.In;
import net.bither.bitherj.core.Tx;
import net.bither.bitherj.core.UnSignTransaction;
import net.bither.bitherj.db.AbstractDb;
import net.bither.bitherj.exception.ScriptException;
import net.bither.bitherj.qrcode.QRCodeUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.util.encoders.Hex;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static net.bither.bitherj.core.AbstractHD.PathType.EXTERNAL_ROOT_PATH;

public class TransactionsUtil {

    private static final Logger log = LoggerFactory.getLogger(TransactionsUtil.class);
    private static final String TX = "tx";
    private static final String BLOCK_COUNT = "block_count";
    private static final String TX_CNT = "tx_cnt";
    // TODO: blockChain.info
    private static final String BLOCK_CHAIN_HEIGHT = "height";
    private static final String BLOCK_CHAIN_TX = "n_tx";
    private static final String BLOCK_CHAIN_TXS = "txs";
    private static final String BLOCK_CHAIN_BLOCK_HEIGHT = "block_height";
    private static final String BLOCK_CHAIN_TX_INDEX = "tx_index";
    private static final String BLOCK_CHAIN_CNT = "n_tx";

    private static final String DATA = "data";
    private static final String ERR_NO = "err_no";
    private static final String TX_HASH = "tx_hash";
    private static final String VALUE = "value";
    private static final String BLOCK_HEIGHT = "block_height";

    private static final int MaxNoTxAddress = 50;

    private static List<UnSignTransaction> unsignTxs = new ArrayList<UnSignTransaction>();

    /**
     *  TODO: get data from blockChain.info
     */
    private  static List<Tx> getTransactionsFromBlockChain(
            JSONObject jsonObject, int storeBlockHeight) throws Exception {
        List<Tx> transactions = new ArrayList<Tx>();
        List<Block> blocks = AbstractDb.blockProvider.getAllBlocks();
        Map<Integer, Integer> blockMapList = new HashMap<Integer, Integer>();
        int minBlockNo = blocks.get(blocks.size() - 1).getBlockNo();
        for (Block block : blocks) {
            blockMapList.put(block.getBlockNo(), block.getBlockTime());
            if (minBlockNo > block.getBlockNo()) {
                minBlockNo = block.getBlockNo();
            }
        }

        if (!jsonObject.isNull(BLOCK_CHAIN_TX)) {
            JSONArray txsArray = jsonObject.getJSONArray(BLOCK_CHAIN_TXS);
            for (int i = 0; i < txsArray.length(); i++) {
                JSONObject txJSON = txsArray.getJSONObject(i);
                if (!txJSON.has(BLOCK_CHAIN_BLOCK_HEIGHT)) {
                    continue;
                }
                int height = txJSON.getInt(BLOCK_CHAIN_BLOCK_HEIGHT);
                if (height > storeBlockHeight && storeBlockHeight > 0) {
                    continue;
                }
                Integer timeKey = height;
                if (height <= minBlockNo) {
                    timeKey = minBlockNo;
                }
                // TODO: get single tx hex format data
                int txIndex = txJSON.getInt(BLOCK_CHAIN_TX_INDEX);

                String txHex = getTxHexByIndex(txIndex);

                byte[] decodeTxHex = Hex.decode(txHex);
                // byte[] txBytes = Base64.encode(decodeTxHex, Base64.DEFAULT);

                Tx tx = new Tx(decodeTxHex);
                tx.setBlockNo(height);

                if (blockMapList.containsKey(timeKey)) {
                    tx.setTxTime(blockMapList.get(timeKey));
                }
                transactions.add(tx);
            }
        }
        return transactions;

    }

    private static String getTxHexByIndex(int txIndex) throws Exception {
        BlockChainMytransactionsApi blockChainMytransactionsApi = new BlockChainMytransactionsApi(txIndex);
        blockChainMytransactionsApi.handleHttpGet();
        String rel = blockChainMytransactionsApi.getResult();
        return rel;
    }
    /**
     *  end
     */


    private static List<Tx> getTransactionsFromBither(
            JSONObject jsonObject, int storeBlockHeight) throws JSONException {
        List<Tx> transactions = new ArrayList<Tx>();
        List<Block> blocks = AbstractDb.blockProvider.getAllBlocks();
        Map<Integer, Integer> blockMapList = new HashMap<Integer, Integer>();
        int minBlockNo = blocks.get(blocks.size() - 1).getBlockNo();
        for (Block block : blocks) {
            blockMapList.put(block.getBlockNo(), block.getBlockTime());
            if (minBlockNo > block.getBlockNo()) {
                minBlockNo = block.getBlockNo();
            }
        }
        if (!jsonObject.isNull(TX)) {
            JSONArray txsArray = jsonObject.getJSONArray(TX);
            for (int i = 0; i < txsArray.length(); i++) {
                JSONArray txArray = txsArray.getJSONArray(i);
                if (txArray.length() < 2) {
                    continue;
                }
                int height = txArray.getInt(0);
                if (height > storeBlockHeight && storeBlockHeight > 0) {
                    continue;
                }
                String txString = txArray.getString(1);
                byte[] txBytes = Base64.decode(txString, Base64.DEFAULT);
                Tx tx = new Tx(txBytes);
                tx.setBlockNo(height);
                Integer timeKey = height;
                if (height <= minBlockNo) {
                    timeKey = minBlockNo;
                }
                if (blockMapList.containsKey(timeKey)) {
                    tx.setTxTime(blockMapList.get(timeKey));
                }
                transactions.add(tx);
            }
        }
        return transactions;

    }

    private static List<Tx> getUnspentTxsFromBither(String address, JSONArray jsonArray, int storeBlockHeight) throws JSONException {
        List<Tx> transactions = new ArrayList<Tx>();
        List<Block> blocks = AbstractDb.blockProvider.getAllBlocks();
        Map<Integer, Integer> blockMapList = new HashMap<Integer, Integer>();
        int minBlockNo = blocks.get(blocks.size() - 1).getBlockNo();
        for (Block block : blocks) {
            blockMapList.put(block.getBlockNo(), block.getBlockTime());
            if (minBlockNo > block.getBlockNo()) {
                minBlockNo = block.getBlockNo();
            }
        }
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            if (jsonObject == null) {
                continue;
            }
            int height = jsonObject.isNull(BLOCK_HEIGHT) ? 0 : jsonObject.getInt(BLOCK_HEIGHT);
            if (height > storeBlockHeight && storeBlockHeight > 0) {
                continue;
            }
            Tx tx = new Tx(jsonObject, address);
            transactions.add(tx);
        }
        return transactions;

    }

    public static List<In> getInSignatureFromBither(String str) {
        List<In> result = new ArrayList<In>();
        if (str.length() > 0) {
            String[] txs = str.split(";");
            for (String tx : txs) {
                String[] ins = tx.split(":");
                byte[] txHash = Utils.reverseBytes(Base64.decode(ins[0], Base64.URL_SAFE));
                for (int i = 1; i < ins.length; i++) {
                    String[] array = ins[i].split(",");
                    int inSn = Integer.decode(array[0]);
                    byte[] inSignature = Base64.decode(array[1], Base64.URL_SAFE);
                    In in = new In();
                    in.setTxHash(txHash);
                    in.setInSn(inSn);
                    in.setInSignature(inSignature);
                    result.add(in);
                }
            }
        }
        return result;
    }

    public static class ComparatorTx implements Comparator<Tx> {

        @Override
        public int compare(Tx lhs, Tx rhs) {
            if (lhs.getBlockNo() != rhs.getBlockNo()) {
                return Integer.valueOf(lhs.getBlockNo()).compareTo(Integer.valueOf(rhs.getBlockNo()));
            } else {
                return Integer.valueOf(lhs.getTxTime()).compareTo(Integer.valueOf(rhs.getTxTime()));
            }

        }

    }

    // TODO display unSignTx

    public static UnSignTransaction getUnsignTxFromCache(String address) {
        synchronized (unsignTxs) {
            for (UnSignTransaction unSignTransaction : unsignTxs) {
                if (Utils.compareString(address,
                        unSignTransaction.getAddress())) {
                    return unSignTransaction;
                }
            }
            return null;
        }

    }

    public static void removeSignTx(UnSignTransaction unSignTransaction) {
        synchronized (unsignTxs) {
            if (unsignTxs.contains(unSignTransaction)) {
                unsignTxs.remove(unSignTransaction);
            }
        }
    }

    public static void addUnSignTxToCache(UnSignTransaction unSignTransaction) {
        synchronized (unsignTxs) {
            if (unsignTxs.contains(unSignTransaction)) {
                unsignTxs.remove(unSignTransaction);
            }
            unsignTxs.add(unSignTransaction);
        }
    }

    public static boolean signTransaction(Tx tx, String qrCodeContent)
            throws ScriptException {
        String[] stringArray = QRCodeUtil.splitString(qrCodeContent);
        List<byte[]> hashList = new ArrayList<byte[]>();
        for (String str : stringArray) {
            if (!Utils.isEmpty(str)) {
                hashList.add(Utils.hexStringToByteArray(str));
            }
        }
        tx.signWithSignatures(hashList);
        return tx.verifySignatures();
    }


    public static void getMyTxFromBither() throws Exception {
        if (AbstractApp.bitherjSetting.getAppMode() != BitherjSettings.AppMode.HOT) {
            return;
        }
        getUnspentTxForAddress();
        if (AddressManager.getInstance().getHdAccount() != null) {
            getHDAccountUnspentAddress(EXTERNAL_ROOT_PATH, 0, MaxNoTxAddress, -1, 0, new ArrayList<HDAccount.HDAccountAddress>());
        }
    }

    private static void getTxForHDAccount(int hdSeedId, final int webType) throws Exception {
        for (AbstractHD.PathType pathType : AbstractHD.PathType.values()) {
            HDAccount.HDAccountAddress hdAccountAddress;
//            boolean hasTx = true;
            int unusedAddressCnt = 0; //HDAccount.MaxUnusedNewAddressCount
            int maxUnusedAddressCount = HDAccount.MaxUnusedNewAddressCount;
            int addressIndex = 0;
            while (unusedAddressCnt <= maxUnusedAddressCount) {
                Block storedBlock = BlockChain.getInstance().getLastBlock();
                int storeBlockHeight = storedBlock.getBlockNo();
                hdAccountAddress = AbstractDb.hdAccountProvider.addressForPath(
                        pathType, addressIndex);
                if (hdAccountAddress == null) {
//                    hasTx = false;
                    unusedAddressCnt += 1;
                    log.warn("hd address is null path {} ,index {}", pathType, addressIndex);
                    continue;
                }
                if (hdAccountAddress.isSyncedComplete()) {
                    log.info("hd address is synced path {} ,index {}, {}", pathType,
                            addressIndex, hdAccountAddress.getAddress());
                    addressIndex++;
                    continue;
                }
                int apiBlockCount = 0;
                int txSum = 0;
                boolean needGetTxs = true;
                int page = 1;
                // TODO
                List<Tx> transactions;


                log.info("hd address will sync path {} ,index {}, {}", pathType, addressIndex, hdAccountAddress.getAddress());
                while (needGetTxs) {
                    // TODO: get data from bither.net else from blockchain.info
                    if (webType == 0) {
                        JSONObject jsonObject = BitherMytransactionsApi.queryTransactions(hdAccountAddress.getAddress(), page);

                        if (!jsonObject.isNull(BLOCK_COUNT)) {
                            apiBlockCount = jsonObject.getInt(BLOCK_COUNT);
                        }
                        int txCnt = jsonObject.getInt(TX_CNT);
                        transactions = TransactionsUtil.getTransactionsFromBither(jsonObject, storeBlockHeight);
                        transactions = AddressManager.getInstance().compressTxsForHDAccount(transactions);

                        Collections.sort(transactions, new ComparatorTx());
                        // address.initTxs(transactions);
                        AddressManager.getInstance().getHdAccount().initTxs(transactions);
                        txSum = txSum + transactions.size();
                        needGetTxs = transactions.size() > 0;
                        page++;

                    }else {
                        BlockChainMytransactionsApi blockChainMytransactionsApi = new BlockChainMytransactionsApi(hdAccountAddress.getAddress());
                        blockChainMytransactionsApi.handleHttpGet();
                        String txResult = blockChainMytransactionsApi.getResult();
                        JSONObject jsonObject = new JSONObject(txResult);
                        // TODO: get the latest block number from blockChain.info
                        JSONObject jsonObjectBlockChain = getLatestBlockNumberFromBlockchain();
                        if (!jsonObjectBlockChain.isNull(BLOCK_CHAIN_HEIGHT)) {
                            apiBlockCount = jsonObjectBlockChain.getInt(BLOCK_CHAIN_HEIGHT);
                        }
                        int txCnt = jsonObject.getInt(BLOCK_CHAIN_CNT);
                        // TODO: get transactions from blockChain.info
                        transactions = TransactionsUtil.getTransactionsFromBlockChain(jsonObject, storeBlockHeight);
                        transactions = AddressManager.getInstance().compressTxsForHDAccount(transactions);

                        Collections.sort(transactions, new ComparatorTx());
                        // address.initTxs(transactions);
                        AddressManager.getInstance().getHdAccount().initTxs(transactions);
                        txSum = txSum + transactions.size();
                        needGetTxs = false;

                    }
                }
                /*
                while (needGetTxs) {
                    JSONObject jsonObject = BitherMytransactionsApi.queryTransactions(hdAccountAddress.getAddress(), page);
                    if (!jsonObject.isNull(BLOCK_COUNT)) {
                        apiBlockCount = jsonObject.getInt(BLOCK_COUNT);
                    }
                    int txCnt = jsonObject.getInt(TX_CNT);
                    List<Tx> transactions = TransactionsUtil.getTransactionsFromBither(
                            jsonObject, storeBlockHeight);
                    transactions = AddressManager.getInstance().compressTxsForHDAccount(transactions);
                    Collections.sort(transactions, new ComparatorTx());
                    AddressManager.getInstance().getHdAccount().initTxs(transactions);
                    txSum = txSum + transactions.size();
                    needGetTxs = transactions.size() > 0;
                    page++;
                }
                */
                if (apiBlockCount < storeBlockHeight && storeBlockHeight - apiBlockCount < 100) {
                    BlockChain.getInstance().rollbackBlock(apiBlockCount);
                }

                log.info("hd address did sync {} tx, path {} ,index {}, {}", txSum, pathType, addressIndex, hdAccountAddress.getAddress());
                hdAccountAddress.setSyncedComplete(true);
                AddressManager.getInstance().getHdAccount().updateSyncComplete(hdAccountAddress);

                if (txSum > 0) {
                    if (pathType == AbstractHD.PathType.EXTERNAL_ROOT_PATH) {
                        AddressManager.getInstance().getHdAccount().updateIssuedExternalIndex(addressIndex);
                    } else {
                        AddressManager.getInstance().getHdAccount().updateIssuedInternalIndex(addressIndex);
                    }
                    AddressManager.getInstance().getHdAccount().supplyEnoughKeys(false);
//                    hasTx = true;
                    unusedAddressCnt = 0;
                } else {
//                    hasTx = false;
                    unusedAddressCnt += 1;
                }
                addressIndex++;
            }
            AbstractDb.hdAccountProvider.updateSyncdForIndex(pathType, addressIndex - 1);
        }
    }

    private static void getTxForAddress(final int webType) throws Exception {
        for (Address address : AddressManager.getInstance().getAllAddresses()) {
            Block storedBlock = BlockChain.getInstance().getLastBlock();
            int storeBlockHeight = storedBlock.getBlockNo();
            if (!address.isSyncComplete()) {
                int apiBlockCount = 0;
                int txSum = 0;
                boolean needGetTxs = true;
                int page = 1;
                // TODO
                List<Tx> transactions = new ArrayList<Tx>();

                while (needGetTxs) {
                    // TODO: get data from bither.net else from blockchain.info
                    if (webType == 0) {
                        JSONObject jsonObject = BitherMytransactionsApi.queryTransactions(address.getAddress(), page);

                        if (!jsonObject.isNull(BLOCK_COUNT)) {
                            apiBlockCount = jsonObject.getInt(BLOCK_COUNT);
                        }
                        int txCnt = jsonObject.getInt(TX_CNT);
                        transactions = TransactionsUtil.getTransactionsFromBither(jsonObject, storeBlockHeight);
                        transactions = AddressManager.getInstance().compressTxsForApi(transactions, address);

                        Collections.sort(transactions, new ComparatorTx());
                        address.initTxs(transactions);
                        txSum = txSum + transactions.size();
                        needGetTxs = transactions.size() > 0;
                        page++;

                    } else {
                        BlockChainMytransactionsApi blockChainMytransactionsApi = new BlockChainMytransactionsApi(address.getAddress());
                        blockChainMytransactionsApi.handleHttpGet();
                        String txResult = blockChainMytransactionsApi.getResult();
                        JSONObject jsonObject = new JSONObject(txResult);
                        // TODO: get the latest block number from blockChain.info
                        JSONObject jsonObjectBlockChain = getLatestBlockNumberFromBlockchain();
                        if (!jsonObjectBlockChain.isNull(BLOCK_CHAIN_HEIGHT)) {
                            apiBlockCount = jsonObjectBlockChain.getInt(BLOCK_CHAIN_HEIGHT);
                        }
                        int txCnt = jsonObject.getInt(BLOCK_CHAIN_CNT);
                        // TODO: get transactions from blockChain.info
                        transactions = TransactionsUtil.getTransactionsFromBlockChain(jsonObject, storeBlockHeight);
                        transactions = AddressManager.getInstance().compressTxsForApi(transactions, address);

                        Collections.sort(transactions, new ComparatorTx());
                        address.initTxs(transactions);
                        txSum = txSum + transactions.size();
                        needGetTxs = false;
                        /*
                    Collections.sort(transactions, new ComparatorTx());
                    address.initTxs(transactions);
                    txSum = txSum + transactions.size();
                    needGetTxs = transactions.size() > 0;
                    page++;
                    */
                    }

                    if (apiBlockCount < storeBlockHeight && storeBlockHeight - apiBlockCount < 100) {
                        BlockChain.getInstance().rollbackBlock(apiBlockCount);
                    }
                    address.setSyncComplete(true);
                    if (address instanceof HDMAddress) {
                        HDMAddress hdmAddress = (HDMAddress) address;
                        hdmAddress.
                                updateSyncComplete();
                    } else {
                        address.updateSyncComplete();
                    }
                }
            }
        }

    }

    // TODO: get the latest block info of JSON format from blockChain.info
    private static JSONObject getLatestBlockNumberFromBlockchain() throws Exception {
        BlockChainMytransactionsApi blockChainMytransactionsApi = new BlockChainMytransactionsApi();
        blockChainMytransactionsApi.handleHttpGet();
        String txResultBlockChain = blockChainMytransactionsApi.getResult();
        return new JSONObject(txResultBlockChain);
    }

    /// unspent tx

    private static void getHDAccountUnspentAddress(final AbstractHD.PathType pathType, final int beginIndex, final int endIndex, int lastTxIndex, int unusedAddressCnt, ArrayList<HDAccount.HDAccountAddress> unspentAddresses) throws Exception {
        String addressesStr = "";
        ArrayList<HDAccount.HDAccountAddress> queryHdAccountAddressList = new ArrayList<HDAccount.HDAccountAddress>();
        for (int i = beginIndex; i < endIndex; i++) {
            HDAccount.HDAccountAddress hdAccountAddress = AbstractDb.hdAccountProvider.addressForPath(
                    pathType, i);
            if (hdAccountAddress == null) {
                unusedAddressCnt += 1;
                if (unusedAddressCnt > HDAccount.MaxUnusedNewAddressCount) {
                    if (pathType.nextPathType() != null) {
                        getHDAccountUnspentAddress(pathType.nextPathType(), 0, MaxNoTxAddress, -1, 0, unspentAddresses);
                    } else {
                        getUnspentTxForHDAccount(unspentAddresses);
                    }
                    AbstractDb.hdAccountProvider.updateSyncdForIndex(pathType, endIndex - 1);
                    return;
                }
                log.warn("hd address is null path {} ,index {}", pathType, i);
                continue;
            }
            if (hdAccountAddress.isSyncedComplete()) {
                log.info("hd address is synced path {} ,index {}, {}", pathType,
                        i, hdAccountAddress.getAddress());
                continue;
            }
            queryHdAccountAddressList.add(hdAccountAddress);
            if (addressesStr.equals("")) {
                addressesStr = hdAccountAddress.getAddress();
            } else {
                addressesStr = addressesStr + "," + hdAccountAddress.getAddress();
            }
        }
        if (addressesStr.equals("")) {
            getHDAccountUnspentAddress(pathType, endIndex, MaxNoTxAddress + endIndex, lastTxIndex, unusedAddressCnt, unspentAddresses);
            return;
        }
        JSONObject jsonObject = BitherQueryAddressApi.queryAddress(addressesStr);
        boolean isNoTxAddress = jsonObject == null || jsonObject.isNull(ERR_NO) || jsonObject.getInt(ERR_NO) != 0 || jsonObject.isNull(DATA);
        if (!isNoTxAddress) {
            try {
                isNoTxAddress = isNoTxAddress || jsonObject.getString(DATA).equals("null");
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        if (isNoTxAddress) {
            nextHDAccountUnspentAddress(queryHdAccountAddressList, pathType, endIndex, lastTxIndex, unusedAddressCnt, unspentAddresses);
            return;
        }

        JSONArray addrJsonArray = new JSONArray();
        if (addressesStr.contains(",")) {
            addrJsonArray = jsonObject.getJSONArray(DATA);
        } else {
            addrJsonArray.put(jsonObject.getJSONObject(DATA));
        }
        for (int i = 0; i < addrJsonArray.length(); i++) {
            JSONObject addrJsonObject;
            try {
                addrJsonObject = addrJsonArray.getJSONObject(i);
            } catch (Exception ex) {
                ex.printStackTrace();
                continue;
            }
            if (addrJsonObject == null || addrJsonObject.isNull("address")) {
                continue;
            }
            String address = addrJsonObject.getString("address");
            for (HDAccount.HDAccountAddress hdAccountAddress: queryHdAccountAddressList) {
                if (hdAccountAddress.getAddress().equals(address)) {
                    boolean hasTx = addrJsonObject.getInt("tx_count") > 0;
                    if (addrJsonObject.getLong("balance") > 0) {
                        unspentAddresses.add(hdAccountAddress);
                    } else {
                        updateHdAccountAddress(hdAccountAddress, hasTx);
                    }
                    if (hasTx) {
                        lastTxIndex = hdAccountAddress.getIndex();
                    }
                    queryHdAccountAddressList.remove(hdAccountAddress);
                    break;
                }
            }
        }
        nextHDAccountUnspentAddress(queryHdAccountAddressList, pathType, endIndex, lastTxIndex, unusedAddressCnt, unspentAddresses);
    }

    private static void nextHDAccountUnspentAddress(ArrayList<HDAccount.HDAccountAddress> queryHdAccountAddressList, final AbstractHD.PathType pathType, final int endIndex, int lastTxIndex, int unusedAddressCnt, ArrayList<HDAccount.HDAccountAddress> unspentAddresses) throws Exception {
        if (queryHdAccountAddressList.size() > 0) {
            for (HDAccount.HDAccountAddress hdAccountAddress: queryHdAccountAddressList) {
                updateHdAccountAddress(hdAccountAddress, false);
            }
        }

        if (lastTxIndex + MaxNoTxAddress > endIndex) {
            getHDAccountUnspentAddress(pathType, endIndex, MaxNoTxAddress + lastTxIndex, lastTxIndex, unusedAddressCnt, unspentAddresses);
        } else {
            AbstractDb.hdAccountProvider.updateSyncdForIndex(pathType, endIndex - 1);
            if (pathType.nextPathType() != null) {
                getHDAccountUnspentAddress(pathType.nextPathType(), 0, MaxNoTxAddress, -1, 0, unspentAddresses);
            } else {
                getUnspentTxForHDAccount(unspentAddresses);
            }
        }
    }

    private static void getUnspentTxForAddress() throws Exception {
        for (Address address : AddressManager.getInstance().getAllAddresses()) {
            Block storedBlock = BlockChain.getInstance().getLastBlock();
            int storeBlockHeight = storedBlock.getBlockNo();
            if (!address.isSyncComplete()) {
                int apiBlockCount = 0;
                boolean needGetTxs = true;
                int page = 1;
                List<Tx> transactions;
                while (needGetTxs) {
                    JSONObject unspentsJsonObject = BitherQueryAddressUnspentApi.queryAddressUnspent(address.getAddress(), page);
                    transactions = getUnspentTransactions(address.getAddress(), unspentsJsonObject, storeBlockHeight);
                    if (transactions.size() > 0) {
                        Tx tx = transactions.get(transactions.size() - 1);
                        if (tx.getBlockNo() > apiBlockCount) {
                            apiBlockCount = tx.getBlockNo();
                        }
                    }
                    transactions = AddressManager.getInstance().compressTxsForApi(transactions, address);
                    Collections.sort(transactions, new ComparatorTx());
                    address.initTxs(transactions);
                    needGetTxs = getNeedGetTxs(unspentsJsonObject, page, transactions);
                    page++;
                }

                if (apiBlockCount < storeBlockHeight && storeBlockHeight - apiBlockCount < 100) {
                    BlockChain.getInstance().rollbackBlock(apiBlockCount);
                }
                address.setSyncComplete(true);
                if (address instanceof HDMAddress) {
                    HDMAddress hdmAddress = (HDMAddress) address;
                    hdmAddress.updateSyncComplete();
                } else {
                    address.updateSyncComplete();
                }
            }
        }
    }

    private static void getUnspentTxForHDAccount(ArrayList<HDAccount.HDAccountAddress> unspentAddresses) throws Exception {
        for (int i = 0; i < unspentAddresses.size(); i++) {
            HDAccount.HDAccountAddress hdAccountAddress = unspentAddresses.get(i);
            Block storedBlock = BlockChain.getInstance().getLastBlock();
            int storeBlockHeight = storedBlock.getBlockNo();
            int apiBlockCount = 0;
            boolean needGetTxs = true;
            int page = 1;
            List<Tx> transactions;
            while (needGetTxs) {
                JSONObject unspentsJsonObject = BitherQueryAddressUnspentApi.queryAddressUnspent(hdAccountAddress.getAddress(), page);
                transactions = getUnspentTransactions(hdAccountAddress.getAddress(), unspentsJsonObject, storeBlockHeight);
                if (transactions.size() > 0) {
                    Tx tx = transactions.get(transactions.size() - 1);
                    if (tx.getBlockNo() > apiBlockCount) {
                        apiBlockCount = tx.getBlockNo();
                    }
                }
                transactions = AddressManager.getInstance().compressTxsForHDAccount(transactions);
                Collections.sort(transactions, new ComparatorTx());
                AddressManager.getInstance().getHdAccount().initTxs(transactions);
                needGetTxs = getNeedGetTxs(unspentsJsonObject, page, transactions);
                page++;
            }

            if (apiBlockCount < storeBlockHeight && storeBlockHeight - apiBlockCount < 100) {
                BlockChain.getInstance().rollbackBlock(apiBlockCount);
            }

            updateHdAccountAddress(hdAccountAddress, true);
        }
    }

    private static void updateHdAccountAddress(HDAccount.HDAccountAddress hdAccountAddress, boolean hasTx) {
        hdAccountAddress.setSyncedComplete(true);
        HDAccount hdAccount = AddressManager.getInstance().getHdAccount();
        hdAccount.updateSyncComplete(hdAccountAddress);
        if (hasTx) {
            if (hdAccountAddress.getPathType() == EXTERNAL_ROOT_PATH) {
                hdAccount.updateIssuedExternalIndex(hdAccountAddress.getIndex());
            } else {
                hdAccount.updateIssuedInternalIndex(hdAccountAddress.getIndex());
            }
            hdAccount.supplyEnoughKeys(false);
        }
    }

    private static List<Tx> getUnspentTransactions(String address, JSONObject unspentsJsonObject, int storeBlockHeight) throws Exception {
        List<Tx> transactions = new ArrayList<Tx>();
        if (unspentsJsonObject == null ||
                unspentsJsonObject.isNull(ERR_NO) ||
                unspentsJsonObject.getInt(ERR_NO) != 0 ||
                unspentsJsonObject.isNull(DATA)) {
            return transactions;
        }
        JSONArray unspentJsonArray;
        try {
            unspentJsonArray = unspentsJsonObject.getJSONObject(DATA).getJSONArray("list");
        } catch (Exception ex) {
            ex.printStackTrace();
            return transactions;
        }
        if (unspentJsonArray == null || unspentJsonArray.length() == 0) {
            return transactions;
        }

        String txHashs = "";
        for (int i = 0; i < unspentJsonArray.length(); i++) {
            JSONObject unspentJson = unspentJsonArray.getJSONObject(i);
            if (!unspentJson.isNull(TX_HASH) && !Utils.isEmpty(unspentJson.getString(TX_HASH)) && !unspentJson.isNull(VALUE) && unspentJson.getLong(VALUE) > 0) {
                String txHash = unspentJson.getString(TX_HASH);
                if (txHashs.length() > 0) {
                    if (!txHashs.contains(txHash)) {
                        txHashs = txHashs + "," + txHash;
                    }
                } else {
                    txHashs = txHash;
                }
            }
        }
        if (txHashs.equals("")) {
            return transactions;
        }
        JSONObject txsJsonObject = BitherUnspentTxsApi.getUnspentTxs(txHashs);
        if (txsJsonObject.isNull(ERR_NO) || txsJsonObject.getInt(ERR_NO) != 0) {
            throw new Exception("error");
        }
        JSONArray jsonArray = new JSONArray();
        if (txHashs.contains(",")) {
            jsonArray = txsJsonObject.getJSONArray(DATA);
        } else {
            jsonArray.put(txsJsonObject.getJSONObject(DATA));
        }
        transactions = TransactionsUtil.getUnspentTxsFromBither(address, jsonArray, storeBlockHeight);
        return transactions;
    }

    private static boolean getNeedGetTxs(JSONObject unspentsJsonObject, int page, List<Tx> transactions) {
        if (unspentsJsonObject == null || unspentsJsonObject.isNull(DATA)) {
            return false;
        }
        JSONObject dataJsonObject = unspentsJsonObject.getJSONObject(DATA);
        if (!dataJsonObject.isNull("pagesize") && !dataJsonObject.isNull("total_count")) {
            return dataJsonObject.getInt("pagesize") * (page - 1) + transactions.size() < dataJsonObject.getInt("total_count");
        } else  {
            return transactions.size() > 0;
        }
    }

}
