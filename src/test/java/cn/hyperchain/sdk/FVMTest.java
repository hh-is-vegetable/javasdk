package cn.hyperchain.sdk;

import cn.hyperchain.sdk.account.Account;
import cn.hyperchain.sdk.account.Algo;
import cn.hyperchain.sdk.common.utils.*;
import cn.hyperchain.sdk.exception.RequestException;
import cn.hyperchain.sdk.fvm.scale.ScaleCodecReader;
import cn.hyperchain.sdk.fvm.scale.ScaleCodecWriter;
import cn.hyperchain.sdk.fvm.scale.ScaleExtract;
import cn.hyperchain.sdk.fvm.types.FVMType;
import cn.hyperchain.sdk.fvm.types.FixedLengthListType;
import cn.hyperchain.sdk.fvm.types.PrimitiveType;
import cn.hyperchain.sdk.fvm.types.StructType;
import cn.hyperchain.sdk.fvm.types.UnfixedLengthListType;
import cn.hyperchain.sdk.provider.DefaultHttpProvider;
import cn.hyperchain.sdk.provider.ProviderManager;
import cn.hyperchain.sdk.response.ReceiptResponse;
import cn.hyperchain.sdk.service.AccountService;
import cn.hyperchain.sdk.service.ContractService;
import cn.hyperchain.sdk.service.ServiceManager;
import cn.hyperchain.sdk.transaction.Transaction;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

public class FVMTest {
    private static String DEFAULT_URL = "localhost:8081";
    private static String PASSWORD = "123";

    @Test
    public void testFVM() throws IOException, RequestException, DecoderException {
        // 1. build provider manager
        DefaultHttpProvider defaultHttpProvider =
                new DefaultHttpProvider.Builder().setUrl(DEFAULT_URL).build();
        ProviderManager providerManager =
                ProviderManager.createManager(defaultHttpProvider);

        // 2. build service
        ContractService contractService =
                ServiceManager.getContractService(providerManager);
        AccountService accountService =
                ServiceManager.getAccountService(providerManager);

        // 3. build transaction
        Account account = accountService.genAccount(Algo.SMAES, PASSWORD);

        InputStream inputStream1 =
                Thread.currentThread().getContextClassLoader().getResourceAsStream(
                        "fvm-contract/set_hash/SetHash-gc.wasm");
        InputStream inputStream2 =
                Thread.currentThread().getContextClassLoader().getResourceAsStream(
                        "fvm-contract/set_hash/contract.json");
        String abiStr = FileUtil.readFile(inputStream2);
        FVMAbi abi = FVMAbi.fromJson(abiStr);
        Transaction transaction = new Transaction.FVMBuilder(account.getAddress())
                .deploy(inputStream1)
                .build();
        transaction.sign(account);
        ReceiptResponse receiptResponse =
                contractService.deploy(transaction).send().polling();
        String contractAddress = receiptResponse.getContractAddress();
        Assert.assertEquals(transaction.getTransactionHash(),
                receiptResponse.getTxHash());

        // call set_hash(key, value)
        // String payload_set =
        // "207365745f68617368186b65793030316474686973206973207468652076616c7565206f662030303031";
        FuncParams params = new FuncParams();
        params.addParams("key001");
        params.addParams("this is the value of 0001");
        Transaction transaction1 =
                new Transaction.FVMBuilder(account.getAddress())
                        .invoke(contractAddress, "set_hash", abi, params)
                        .build();
        transaction1.sign(account);
        ReceiptResponse receiptResponse1 =
                contractService.invoke(transaction1).send().polling();

        // call get_hash(key)
        // String payload_get = "206765745f68617368186b6579303031";
        FuncParams params2 = new FuncParams();
        params2.addParams("key001");
        Transaction transaction2 =
                new Transaction.FVMBuilder(account.getAddress())
                        .invoke(contractAddress, "get_hash", abi, params2)
                        .build();
        transaction2.sign(account);
        ReceiptResponse receiptResponse2 =
                contractService.invoke(transaction2).send().polling();

        String aaa = ScaleExtract.fromBytesArray(ScaleCodecReader.STRING)
                .apply(Hex.decodeHex(
                        Utils.deleteHexPre(receiptResponse2.getRet())));

        //
        // FuncParams params1 = new FuncParams();
        // params1.addParams("1");
        // Transaction transaction1 = new
        // Transaction.EVMBuilder(account.getAddress()).invoke(contractAddress,
        // "TestBytes32(bytes32)", abi, params1).build();
        // transaction1.sign(account);
        // ReceiptResponse receiptResponse1 =
        // contractService.invoke(transaction1).send().polling();
        // System.out.println(receiptResponse1.getRet());
        // Assert.assertEquals(transaction1.getTransactionHash(),
        // receiptResponse1.getTxHash());
        // List decodeList =
        // abi.getFunction("TestBytes32(bytes32)").decodeResult(ByteUtil.fromHex(receiptResponse1.getRet()));
        // String[] topics = receiptResponse1.getLog()[0].getTopics();
        // byte[][] topicsData = new byte[topics.length][];
        // for (int i = 0; i < topics.length; i ++) {
        // topicsData[i] = ByteUtil.fromHex(topics[i]);
        // }
        // List decodeLogList =
        // abi.getEvent("eventA(bytes,string)").decode(ByteUtil.fromHex(receiptResponse1.getLog()[0].getData()),
        // topicsData);
        // for (Object o : decodeList) {
        // System.out.println(o.getClass());
        // System.out.println(new String((byte[]) o));
        // }
        // System.out.println("---");
        // for (Object o : decodeLogList) {
        // System.out.println(o.getClass());
        // System.out.println(new String((byte[]) o));
        // }
        //
        // System.out.println("*********************************************************************");
        // // maintain contract test
        //
        // // test freeze
        // Transaction transaction2 = new
        // Transaction.EVMBuilder(account.getAddress()).upgrade(contractAddress,
        // bin).build();
        // transaction2.sign(account);
        // ReceiptResponse receiptResponse2 =
        // contractService.maintain(transaction2).send().polling();
        // System.out.println(receiptResponse2.getRet());
        //
        // // test thaw
        // Transaction transaction3 = new
        // Transaction.EVMBuilder(account.getAddress()).freeze(contractAddress).build();
        // transaction3.sign(account);
        // ReceiptResponse receiptResponse3 =
        // contractService.maintain(transaction3).send().polling();
        // System.out.println(receiptResponse3.getRet());
        //
        // // test upgrade
        // Transaction transaction4 = new
        // Transaction.EVMBuilder(account.getAddress()).unfreeze(contractAddress).build();
        // transaction4.sign(account);
        // ReceiptResponse receiptResponse4 =
        // contractService.maintain(transaction4).send().polling();
        // System.out.println(receiptResponse4.getRet());
    }

    // 测试FVM合约的高级写法以及调用
    @Test
    @Ignore
    public void testFVMAdvanced() throws RequestException {
        // 1. build provider manager
        DefaultHttpProvider defaultHttpProvider =
                new DefaultHttpProvider.Builder().setUrl(DEFAULT_URL).build();
        ProviderManager providerManager =
                ProviderManager.createManager(defaultHttpProvider);

        // 2. build service
        ContractService contractService =
                ServiceManager.getContractService(providerManager);
        AccountService accountService =
                ServiceManager.getAccountService(providerManager);

        // 3. build transaction
        Account account = accountService.genAccount(Algo.SMAES, PASSWORD);

        InputStream inputStream1 =
                Thread.currentThread().getContextClassLoader().getResourceAsStream(
                        "fvm-contract/set_hash_advance/SetHash-Advanced.wasm");
        Transaction transaction = new Transaction.FVMBuilder(account.getAddress())
                .deploy(inputStream1)
                .build();
        transaction.sign(account);
        ReceiptResponse receiptResponse =
                contractService.deploy(transaction).send().polling();
        String contractAddress = receiptResponse.getContractAddress();
        Assert.assertEquals(transaction.getTransactionHash(),
                receiptResponse.getTxHash());

        String set_hash_signature = "d7fa1007";
        String get_hash_signature = "3cf5040a";

        String key = stringToHashString("key001");
        String value = stringToHashString("this is the value of 0001");

        // call set_hash
        String payloadSetHash = set_hash_signature + key + value;
        Transaction transaction1 = new Transaction.FVMBuilder(account.getAddress())
                .invoke(contractAddress, payloadSetHash)
                .build();
        transaction1.sign(account);
        ReceiptResponse receiptResponse1 =
                contractService.invoke(transaction1).send().polling();

        // call get_hash(key)
        String payloadGetHash = get_hash_signature + key;
        Transaction transaction2 = new Transaction.FVMBuilder(account.getAddress())
                .invoke(contractAddress, payloadGetHash)
                .build();
        transaction2.sign(account);
        ReceiptResponse receiptResponse2 =
                contractService.invoke(transaction2).send().polling();
    }

    @Test
    public void testFVMCodec1() throws IOException {
        InputStream inputStream =
                Thread.currentThread().getContextClassLoader().getResourceAsStream(
                        "fvm-abi/test-abi-1.json");
        String abiStr = FileUtil.readFile(inputStream);
        FVMAbi abi = FVMAbi.fromJson(abiStr);

        // build args
        List<List<String>> arg13 = new ArrayList<>();
        List<String> arg111 = new ArrayList<>();
        arg111.add("eee");
        arg111.add("fff");
        arg111.add("ggg");
        arg13.add(arg111);
        arg13.add(arg111);
        arg13.add(arg111);

        List arg1 = new ArrayList<>();
        arg1.add(133);
        arg1.add("abc");
        arg1.add(arg13);

        // encode
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        ScaleCodecWriter codecWriter = new ScaleCodecWriter(buf);
        abi.getMethods().get("test-1").input.get(0).encode(codecWriter, arg1);

        // decode
        ScaleCodecReader codecReader = new ScaleCodecReader(buf.toByteArray());
        Object result =
                abi.getMethods().get("test-1").output.get(0).decode(codecReader);
    }

    @Test
    public void testFVMCodec2() throws IOException {
        InputStream inputStream =
                Thread.currentThread().getContextClassLoader().getResourceAsStream(
                        "fvm-abi/test-abi-2.json");
        String abiStr = FileUtil.readFile(inputStream);
        FVMAbi abi = FVMAbi.fromJson(abiStr);

        // build args
        List<List<String>> arg13 = new ArrayList<>();
        List<String> arg111 = new ArrayList<>();
        arg111.add("eee");
        arg111.add("fff");
        arg111.add("ggg");
        arg13.add(arg111);
        arg13.add(arg111);
        arg13.add(arg111);

        List arg1 = new ArrayList<>();
        arg1.add(133);
        arg1.add("abc");
        arg1.add(arg13);

        // encode
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        ScaleCodecWriter codecWriter = new ScaleCodecWriter(buf);
        abi.getMethods().get("test-1").input.get(0).encode(codecWriter, arg1);

        // decode
        ScaleCodecReader codecReader = new ScaleCodecReader(buf.toByteArray());
        Object result =
                abi.getMethods().get("test-1").output.get(0).decode(codecReader);
    }

    @Test
    public void testFVMTypes() throws IOException, RequestException {
        // InputStream inputStream =
        // Thread.currentThread().getContextClassLoader().getResourceAsStream("fvm-contract/types/contract.json");
        // String abiStr = FileUtil.readFile(inputStream);
        // FVMAbi abi = FVMAbi.fromJson(abiStr);
        //
        // // build args
        // List arg1 = new ArrayList();
        // arg1.add(2);
        // List arg2 = new ArrayList();
        // arg2.add(true);
        // List arg3 = new ArrayList();
        // arg3.add(arg1);
        // List arg4 = new ArrayList();
        // arg4.add(arg2);
        //
        // List arg = new ArrayList<>();
        // for(int i = 0; i < 10; i++) {
        // arg.add(arg1);
        // }
        // arg.add(arg2);
        // for(int i = 0; i < 10; i++) {
        // arg.add(arg3);
        // }
        // arg.add(arg4);
        //
        // // encode
        // ByteArrayOutputStream buf = new ByteArrayOutputStream();
        // ScaleCodecWriter codecWriter = new ScaleCodecWriter(buf);
        // abi.getMethods().get("make_types").input.get(0).encode(codecWriter, arg);
        //
        // // decode
        // ScaleCodecReader codecReader = new ScaleCodecReader(buf.toByteArray());
        // Object result =
        // abi.getMethods().get("make_types").output.get(0).decode(codecReader);
        // System.out.println(result);

        // 1. build provider manager
        DefaultHttpProvider defaultHttpProvider =
                new DefaultHttpProvider.Builder().setUrl(DEFAULT_URL).build();
        ProviderManager providerManager =
                ProviderManager.createManager(defaultHttpProvider);

        // 2. build service
        ContractService contractService =
                ServiceManager.getContractService(providerManager);
        AccountService accountService =
                ServiceManager.getAccountService(providerManager);

        // 3. build transaction
        Account account = accountService.genAccount(Algo.SMAES, PASSWORD);

        InputStream inputStream1 =
                Thread.currentThread().getContextClassLoader().getResourceAsStream(
                        "fvm-contract/types/types_gc.wasm");
        InputStream inputStream2 =
                Thread.currentThread().getContextClassLoader().getResourceAsStream(
                        "fvm-contract/types/contract.json");
        String abiStr = FileUtil.readFile(inputStream2);
        FVMAbi abi = FVMAbi.fromJson(abiStr);
        Transaction transaction = new Transaction.FVMBuilder(account.getAddress())
                .deploy(inputStream1)
                .build();
        transaction.sign(account);
        ReceiptResponse receiptResponse =
                contractService.deploy(transaction).send().polling();
        String contractAddress = receiptResponse.getContractAddress();
        // Assert.assertEquals(transaction.getTransactionHash(),
        // receiptResponse.getTxHash());

        // build args
        List arg1 = new ArrayList();
        arg1.add(2);
        List arg2 = new ArrayList();
        arg2.add(true);
        List arg3 = new ArrayList();
        arg3.add(arg1);
        List arg4 = new ArrayList();
        arg4.add(arg2);

        List arg = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            arg.add(arg1);
        }
        arg.add(arg2);
        for (int i = 0; i < 10; i++) {
            arg.add(arg3);
        }
        arg.add(arg4);
        FuncParams params = new FuncParams();
        params.addParams(arg);
        Transaction transaction1 =
                new Transaction.FVMBuilder(account.getAddress())
                        .invoke(contractAddress, "make_types", abi, params)
                        .build();
        transaction1.sign(account);
        ReceiptResponse receiptResponse1 =
                contractService.invoke(transaction1).send().polling();

        Object result = abi.decode(receiptResponse1.getRet(), "make_types");
    }

    @Test
    public void TestFVMContractUpgrade() throws IOException, RequestException {
        // 1. build provider manager
        DefaultHttpProvider defaultHttpProvider =
                new DefaultHttpProvider.Builder().setUrl(DEFAULT_URL).build();
        ProviderManager providerManager =
                ProviderManager.createManager(defaultHttpProvider);

        // 2. build service
        ContractService contractService =
                ServiceManager.getContractService(providerManager);
        AccountService accountService =
                ServiceManager.getAccountService(providerManager);

        // 3. build transaction
        Account account = accountService.genAccount(Algo.SMAES, PASSWORD);

        InputStream inputStream1 =
                Thread.currentThread().getContextClassLoader().getResourceAsStream(
                        "fvm-contract/upgrade/before.wasm");
        InputStream inputStream2 =
                Thread.currentThread().getContextClassLoader().getResourceAsStream(
                        "fvm-contract/upgrade/contract.json");
        String abiStr = FileUtil.readFile(inputStream2);
        FVMAbi abi = FVMAbi.fromJson(abiStr);
        Transaction transaction = new Transaction.FVMBuilder(account.getAddress())
                .deploy(inputStream1)
                .build();
        transaction.sign(account);
        ReceiptResponse receiptResponse =
                contractService.deploy(transaction).send().polling();
        String contractAddress = receiptResponse.getContractAddress();
        Assert.assertEquals(transaction.getTransactionHash(),
                receiptResponse.getTxHash());

        // call set(5)
        FuncParams params = new FuncParams();
        params.addParams(5);
        Transaction transaction1 = new Transaction.FVMBuilder(account.getAddress())
                .invoke(contractAddress, "set", abi, params)
                .build();
        transaction1.sign(account);
        ReceiptResponse receiptResponse1 =
                contractService.invoke(transaction1).send().polling();

        // call get()
        Transaction transaction2 =
                new Transaction.FVMBuilder(account.getAddress())
                        .invoke(contractAddress, "get", abi, new FuncParams())
                        .build();
        transaction2.sign(account);
        ReceiptResponse receiptResponse2 =
                contractService.invoke(transaction2).send().polling();
        Object ret = abi.decode(receiptResponse2.getRet(), "get");

        // test upgrade bad contract
        // it should fail because of missed variable "name"
        // InputStream inputStream3 =
        // Thread.currentThread().getContextClassLoader().getResourceAsStream("fvm-contract/upgrade/after-bad.wasm");
        // InputStream inputStream4 =
        // Thread.currentThread().getContextClassLoader().getResourceAsStream("fvm-contract/upgrade/contract.json");
        // String abiStr2 = FileUtil.readFile(inputStream4);
        // FVMAbi abi2 = FVMAbi.fromJson(abiStr2);
        // Transaction transaction3 = new
        // Transaction.FVMBuilder(account.getAddress()).upgrade(contractAddress,
        // Encoder.encodeDeployWasm(inputStream3)).build();
        // transaction3.sign(account);
        // ReceiptResponse receiptResponse4 =
        // contractService.maintain(transaction3).send().polling();
        // System.out.println(receiptResponse4.getRet());

        // test upgrade okay contract
        // it should success
        InputStream inputStream3 =
                Thread.currentThread().getContextClassLoader().getResourceAsStream(
                        "fvm-contract/upgrade/after-okay.wasm");
        InputStream inputStream4 =
                Thread.currentThread().getContextClassLoader().getResourceAsStream(
                        "fvm-contract/upgrade/contract-2.json");
        String abiStr2 = FileUtil.readFile(inputStream4);
        FVMAbi abi2 = FVMAbi.fromJson(abiStr2);
        Transaction transaction3 =
                new Transaction.FVMBuilder(account.getAddress())
                        .upgrade(contractAddress, Encoder.encodeDeployWasm(inputStream3))
                        .build();
        transaction3.sign(account);
        ReceiptResponse receiptResponse4 =
                contractService.maintain(transaction3).send().polling();

        // call set_new(10)
        FuncParams params1 = new FuncParams();
        params1.addParams(10);
        Transaction transaction4 =
                new Transaction.FVMBuilder(account.getAddress())
                        .invoke(contractAddress, "set_new", abi2, params1)
                        .build();
        transaction4.sign(account);
        ReceiptResponse receiptResponse5 =
                contractService.invoke(transaction4).send().polling();

        // call get_new()
        Transaction transaction5 =
                new Transaction.FVMBuilder(account.getAddress())
                        .invoke(contractAddress, "get_new", abi2, new FuncParams())
                        .build();
        transaction5.sign(account);
        ReceiptResponse receiptResponse6 =
                contractService.invoke(transaction5).send().polling();
        Object ret2 = abi2.decode(receiptResponse6.getRet(), "get_new");
    }

    @Test
    public void testFVMCppAbi()
            throws IOException, RequestException, DecoderException {
        // 1. build provider manager
        DefaultHttpProvider defaultHttpProvider =
                new DefaultHttpProvider.Builder().setUrl(DEFAULT_URL).build();
        ProviderManager providerManager =
                ProviderManager.createManager(defaultHttpProvider);

        // 2. build service
        ContractService contractService =
                ServiceManager.getContractService(providerManager);
        AccountService accountService =
                ServiceManager.getAccountService(providerManager);

        // 3. build transaction
        Account account = accountService.genAccount(Algo.SMAES, PASSWORD);

        InputStream inputStream1 =
                Thread.currentThread().getContextClassLoader().getResourceAsStream(
                        "fvm-contract/fvm_cpp/abi_test.wasm");
        Transaction transaction = new Transaction.FVMBuilder(account.getAddress())
                .deploy(inputStream1)
                .build();
        transaction.sign(account);
        ReceiptResponse receiptResponse =
                contractService.deploy(transaction).send().polling();
        String contractAddress = receiptResponse.getContractAddress();
        Assert.assertEquals(transaction.getTransactionHash(),
                receiptResponse.getTxHash());

        // invoke
        {
            // test string
            String payload =
                    new FVMBuilderParam("set_hash")
                            .addParam(PrimitiveType.getPrimitiveType("String"), "key")
                            .addParam(PrimitiveType.getPrimitiveType("String"), "value")
                            .build();
            Transaction transaction1 =
                    new Transaction.FVMBuilder(account.getAddress())
                            .invoke(contractAddress, payload)
                            .build();
            transaction1.sign(account);
            ReceiptResponse receiptResponse1 =
                    contractService.invoke(transaction1).send().polling();
            System.out.println(receiptResponse1);

            print_log(receiptResponse1);
        }
        {
            System.out.println("get_hash and event");
            String payload =
                    new FVMBuilderParam("get_hash")
                            .addParam(PrimitiveType.getPrimitiveType("String"), "key")
                            .build();
            Transaction transaction1 =
                    new Transaction.FVMBuilder(account.getAddress())
                            .invoke(contractAddress, payload)
                            .build();
            transaction1.sign(account);
            ReceiptResponse receiptResponse1 =
                    contractService.invoke(transaction1).send().polling();
            System.out.println(receiptResponse1);
            print_log(receiptResponse1);

            Object res = FVMAbi.decodeRaw(receiptResponse1.getRet(), new
                    ArrayList<>(Arrays.asList(
                    PrimitiveType.getPrimitiveType("str"))));
            System.out.println(res);
            Assert.assertEquals("value", res);
        }
        {
            System.out.println("struct test");
            String payload =
                    new FVMBuilderParam("test_struct")
                            .addParam(new StructType(new ArrayList<>(Arrays.asList(
                                            PrimitiveType.getPrimitiveType("i8"),
                                            PrimitiveType.getPrimitiveType("u64")))),
                                    new ArrayList<>(Arrays.asList(3, 32)))
                            .build();
            Transaction transaction1 =
                    new Transaction.FVMBuilder(account.getAddress())
                            .invoke(contractAddress, payload)
                            .build();
            transaction1.sign(account);
            ReceiptResponse receiptResponse1 =
                    contractService.invoke(transaction1).send().polling();
            // System.out.println(receiptResponse1);
            Object res =
                    FVMAbi.decodeRaw(receiptResponse1.getRet(),
                            new ArrayList<>(Arrays.asList(
                                    PrimitiveType.getPrimitiveType("u64"))));
            System.out.println(res);
            assert Integer.parseInt(res.toString()) == 35;
        }
        {
            System.out.println("fixed length list type");
            FVMType tp =
                    new FixedLengthListType(PrimitiveType.getPrimitiveType("u64"), 5);
            ArrayList args = new ArrayList<>(Arrays.asList(1, 2, 3, 4, 5));
            String payload =
                    new FVMBuilderParam("test_array").addParam(tp, args).build();
            Transaction transaction1 =
                    new Transaction.FVMBuilder(account.getAddress())
                            .invoke(contractAddress, payload)
                            .build();
            transaction1.sign(account);
            ReceiptResponse receiptResponse1 =
                    contractService.invoke(transaction1).send().polling();
            // System.out.println(receiptResponse1);
            Object res =
                    FVMAbi.decodeRaw(receiptResponse1.getRet(),
                            new ArrayList<>(Arrays.asList(
                                    PrimitiveType.getPrimitiveType("u64"))));
            System.out.println(res);
            assert Integer.parseInt(res.toString()) == 15;
        }
        {
            System.out.println("Unfixed length list type");
            String payload = new FVMBuilderParam("test_vector")
                    .addParam(new UnfixedLengthListType(
                                    PrimitiveType.getPrimitiveType("u32")),
                            new ArrayList<>(Arrays.asList(1, 2, 3)))
                    .build();
            Transaction transaction1 =
                    new Transaction.FVMBuilder(account.getAddress())
                            .invoke(contractAddress, payload)
                            .build();
            transaction1.sign(account);
            ReceiptResponse receiptResponse1 =
                    contractService.invoke(transaction1).send().polling();
            // System.out.println(receiptResponse1);
            Object res =
                    FVMAbi.decodeRaw(receiptResponse1.getRet(),
                            new ArrayList<>(Arrays.asList(
                                    PrimitiveType.getPrimitiveType("u32"))));
            System.out.println(res);
            assert Integer.parseInt(res.toString()) == 6;
        }
        {
            String payload =
                    new FVMBuilderParam("test_complex")
                            .addParam( // std::vector<uint32_t>
                                    new UnfixedLengthListType(
                                            PrimitiveType.getPrimitiveType("u32")),
                                    new ArrayList<>(Arrays.asList(1, 2, 3)))
                            .addParam( // std::array<uint64_t, 3>
                                    new FixedLengthListType(PrimitiveType.getPrimitiveType("u64"),
                                            3),
                                    new ArrayList<>(Arrays.asList(15, 25, 35)))
                            .addParam( // std::string
                                    PrimitiveType.getPrimitiveType("str"),
                                    "good")
                            .addParam(
                                    new StructType(                    // struct
                                            new ArrayList<>(Arrays.asList( // struct field
                                                    // struct
                                                    new StructType(
                                                            new ArrayList<>(Arrays.asList( // struct
                                                                    // field
                                                                    PrimitiveType.getPrimitiveType("u8"),
                                                                    PrimitiveType.getPrimitiveType("u64")))),
                                                    // vector
                                                    new UnfixedLengthListType(
                                                            PrimitiveType.getPrimitiveType("u16")),
                                                    // string
                                                    PrimitiveType.getPrimitiveType("str")))),
                                    new ArrayList<>(
                                            Arrays.asList(new ArrayList<>(Arrays.asList(8, 64)),
                                                    new ArrayList<>(Arrays.asList(3, 4, 5)),
                                                    "struct+vector+string")))
                            .addParam(PrimitiveType.getPrimitiveType("u16"), 273)
                            .build();
            Transaction transaction1 =
                    new Transaction.FVMBuilder(account.getAddress())
                            .invoke(contractAddress, payload)
                            .build();
            transaction1.sign(account);
            ReceiptResponse receiptResponse1 =
                    contractService.invoke(transaction1).send().polling();
            System.out.println(receiptResponse1);
            print_log(receiptResponse1);
        }
        {
            String payload = new FVMBuilderParam("event_err").build();
            Transaction transaction1 =
                    new Transaction.FVMBuilder(account.getAddress())
                            .invoke(contractAddress, payload)
                            .build();
            transaction1.sign(account);
            try {
                ReceiptResponse receiptResponse1 =
                        contractService.invoke(transaction1).send().polling();
                System.out.println(receiptResponse1);
            } catch (Exception e) {
                // handle exception
                System.out.println(e);
            }
        }
        {
            String payload =
                    new FVMBuilderParam("test_primitive")
                            .addParam(PrimitiveType.getPrimitiveType("u8"), 8)
                            .addParam(PrimitiveType.getPrimitiveType("i8"), -8)
                            .addParam(PrimitiveType.getPrimitiveType("u16"), 16)
                            .addParam(PrimitiveType.getPrimitiveType("i16"), -16)
                            .addParam(PrimitiveType.getPrimitiveType("u32"), 32)
                            .addParam(PrimitiveType.getPrimitiveType("i32"), -32)
                            .addParam(PrimitiveType.getPrimitiveType("u64"), 64)
                            .addParam(PrimitiveType.getPrimitiveType("i64"), -64)
                            .addParam(PrimitiveType.getPrimitiveType("str"), "test_primitive")
                            .addParam(PrimitiveType.getPrimitiveType("u64"), 66)
                            .addParam(PrimitiveType.getPrimitiveType("u64"),
                                    1658453919579677200L)
                            .build();
            Transaction transaction1 =
                    new Transaction.FVMBuilder(account.getAddress())
                            .invoke(contractAddress, payload)
                            .build();
            transaction1.sign(account);
            ReceiptResponse receiptResponse1 =
                    contractService.invoke(transaction1).send().polling();
            System.out.println(receiptResponse1);
            print_log(receiptResponse1);
        }
    }

    @Test
    public void testFVMCppInf()
            throws IOException, RequestException, DecoderException {
        // 1. build provider manager
        DefaultHttpProvider defaultHttpProvider =
                new DefaultHttpProvider.Builder().setUrl(DEFAULT_URL).build();
        ProviderManager providerManager =
                ProviderManager.createManager(defaultHttpProvider);

        // 2. build service
        ContractService contractService =
                ServiceManager.getContractService(providerManager);
        AccountService accountService =
                ServiceManager.getAccountService(providerManager);

        // 3. build transaction
        Account account = accountService.genAccount(Algo.SMAES, PASSWORD);
        System.out.println("account Address:" + account.getAddress());

        InputStream inputStream1 =
                Thread.currentThread().getContextClassLoader().getResourceAsStream(
                        "fvm-contract/fvm_cpp/contract_test.wasm");
        Transaction transaction = new Transaction.FVMBuilder(account.getAddress())
                .deploy(inputStream1)
                .build();
        transaction.sign(account);
        ReceiptResponse receiptResponse =
                contractService.deploy(transaction).send().polling();
        String contractAddress = receiptResponse.getContractAddress();
        Assert.assertEquals(transaction.getTransactionHash(),
                receiptResponse.getTxHash());

        // invoke
        {
            String payload = new FVMBuilderParam("get_block_height").build();
            Transaction transaction1 =
                    new Transaction.FVMBuilder(account.getAddress())
                            .invoke(contractAddress, payload)
                            .build();
            transaction1.sign(account);
            ReceiptResponse receiptResponse1 =
                    contractService.invoke(transaction1).send().polling();
            System.out.println(receiptResponse1);
            Object res =
                    FVMAbi.decodeRaw(receiptResponse1.getRet(),
                            new ArrayList<>(Arrays.asList(
                                    PrimitiveType.getPrimitiveType("u64"))));
            System.out.println(res);
        }
        {
            String payload = new FVMBuilderParam("get_block_time").build();
            Transaction transaction1 =
                    new Transaction.FVMBuilder(account.getAddress())
                            .invoke(contractAddress, payload)
                            .build();
            transaction1.sign(account);
            ReceiptResponse receiptResponse1 =
                    contractService.invoke(transaction1).send().polling();
            System.out.println(receiptResponse1);
            Object res =
                    FVMAbi.decodeRaw(receiptResponse1.getRet(),
                            new ArrayList<>(Arrays.asList(
                                    PrimitiveType.getPrimitiveType("u64"))));
            System.out.println(res);
        }
        {
            String payload = new FVMBuilderParam("get_tx_hash").build();
            Transaction transaction1 =
                    new Transaction.FVMBuilder(account.getAddress())
                            .invoke(contractAddress, payload)
                            .build();
            transaction1.sign(account);
            ReceiptResponse receiptResponse1 =
                    contractService.invoke(transaction1).send().polling();
            // System.out.println(receiptResponse1);
            System.out.println(receiptResponse1.getRet());
            assert receiptResponse1.getTxHash().equals(receiptResponse1.getRet());
        }
        {
            String payload = new FVMBuilderParam("get_self_address").build();
            Transaction transaction1 =
                    new Transaction.FVMBuilder(account.getAddress())
                            .invoke(contractAddress, payload)
                            .build();
            transaction1.sign(account);
            ReceiptResponse receiptResponse1 =
                    contractService.invoke(transaction1).send().polling();
            // System.out.println(receiptResponse1);
            System.out.println("get_self_address:" + receiptResponse1.getRet());
            assert receiptResponse1.getRet().equals(contractAddress);
        }
        {
            String payload = new FVMBuilderParam("get_caller_address").build();
            Transaction transaction1 =
                    new Transaction.FVMBuilder(account.getAddress())
                            .invoke(contractAddress, payload)
                            .build();
            transaction1.sign(account);
            ReceiptResponse receiptResponse1 =
                    contractService.invoke(transaction1).send().polling();
            // System.out.println(receiptResponse1);
            System.out.println("get_caller_address:" + receiptResponse1.getRet());
            assert receiptResponse1.getRet().substring(2).equals(
                    account.getAddress());
        }
        {
            String payload = new FVMBuilderParam("get_origin_address").build();
            Transaction transaction1 =
                    new Transaction.FVMBuilder(account.getAddress())
                            .invoke(contractAddress, payload)
                            .build();
            transaction1.sign(account);
            ReceiptResponse receiptResponse1 =
                    contractService.invoke(transaction1).send().polling();
            // System.out.println(receiptResponse1);
            System.out.println("get_origin_address:" + receiptResponse1.getRet());
            assert receiptResponse1.getRet().substring(2).equals(
                    account.getAddress());
        }
    }

    @Test
    public void testFVMCppCmplx()
            throws IOException, RequestException, DecoderException {
        // 1. build provider manager
        DefaultHttpProvider defaultHttpProvider =
                new DefaultHttpProvider.Builder().setUrl(DEFAULT_URL).build();
        ProviderManager providerManager =
                ProviderManager.createManager(defaultHttpProvider);

        // 2. build service
        ContractService contractService =
                ServiceManager.getContractService(providerManager);
        AccountService accountService =
                ServiceManager.getAccountService(providerManager);

        // 3. build transaction
        Account account = accountService.genAccount(Algo.SMAES, PASSWORD);
        System.out.println("account Address:" + account.getAddress());

        InputStream inputStream1 =
                Thread.currentThread().getContextClassLoader().getResourceAsStream(
                        "fvm-contract/fvm_cpp/complex_test.wasm");
        Transaction transaction = new Transaction.FVMBuilder(account.getAddress())
                .deploy(inputStream1)
                .build();
        transaction.sign(account);
        ReceiptResponse receiptResponse =
                contractService.deploy(transaction).send().polling();
        System.out.println("deploy receiptResponse log +++++--+++++\t");
        print_log(receiptResponse);
        String contractAddress = receiptResponse.getContractAddress();
        Assert.assertEquals(transaction.getTransactionHash(),
                receiptResponse.getTxHash());

        // invoke
        { // no such method test
            String payload = new FVMBuilderParam("try_method").build();
            Transaction transaction1 =
                    new Transaction.FVMBuilder(account.getAddress())
                            .invoke(contractAddress, payload)
                            .build();
            transaction1.sign(account);
            try {
                ReceiptResponse receiptResponse1 =
                        contractService.invoke(transaction1).send().polling();
                System.out.println(receiptResponse1);
            } catch (Exception e) {
                System.out.println("exception:");
                System.out.println("\t" + e);
            }
        }
        {
            System.out.println("====get_hash and event");
            String payload =
                    new FVMBuilderParam("get_hash")
                            .addParam(PrimitiveType.getPrimitiveType("String"), "key_init")
                            .build();
            Transaction transaction1 =
                    new Transaction.FVMBuilder(account.getAddress())
                            .invoke(contractAddress, payload)
                            .build();
            transaction1.sign(account);
            ReceiptResponse receiptResponse1 =
                    contractService.invoke(transaction1).send().polling();
            System.out.println(receiptResponse1);
            print_log(receiptResponse1);

            Object res =
                    FVMAbi.decodeRaw(receiptResponse1.getRet(),
                            new ArrayList<>(Arrays.asList(
                                    PrimitiveType.getPrimitiveType("str"))));
            System.out.println(res);
            Assert.assertEquals("value_init", res);
        }

        for(int loop_size = 0; loop_size < 3; loop_size++)
        {
            System.out.println("start test ----------------------------------------------------");
            Random rand =new Random(loop_size);
            String payload = new FVMBuilderParam("test_put_get_mod")
                    .addParam(PrimitiveType.getPrimitiveType("u64"), rand.nextInt(20)+1)
                    .addParam(PrimitiveType.getPrimitiveType("u64"), rand.nextInt(20)+1)
                    .addParam(PrimitiveType.getPrimitiveType("u64"), rand.nextInt(20)+1)
                    .addParam(PrimitiveType.getPrimitiveType("u64"), rand.nextInt(20)+1)
                    .build();
            Transaction transaction1 =
                    new Transaction.FVMBuilder(account.getAddress())
                            .invoke(contractAddress, payload)
                            .build();
            transaction1.sign(account);
            try {
                ReceiptResponse receiptResponse1 =
                        contractService.invoke(transaction1).send().polling();
                System.out.println(receiptResponse1);
                print_log(receiptResponse1);
            } catch (Exception e) {
                System.out.println(e);
            }
//       Thread.sleep(2000);
        }
    }

    public static void print_log(ReceiptResponse receiptResponse1) {
        for (int i = 0; i < receiptResponse1.getLog().length; i++) {
            System.out.println("address\t" +
                    receiptResponse1.getLog()[i].getAddress());
            System.out.println(
                    "data\t" +
                            ByteUtil.decodeHex(receiptResponse1.getLog()[i].getData()));
            for (int j = 0; j < receiptResponse1.getLog()[i].getTopics().length;
                 j++) {
                System.out.println(
                        "topic" + j + "\t" +
                                ByteUtil.decodeHex(
                                        receiptResponse1.getLog()[i].getTopics()[j]));
            }
        }
    }

    public static String stringToHashString(String in) {
        if (in.toLowerCase().startsWith("0x")) {
            in = in.substring(2);
        }
        int len = in.getBytes().length < 32 ? in.getBytes().length : 0;
        byte[] b1 = new byte[32];
        System.arraycopy(in.getBytes(), 0, b1, 0, len);
        return Hex.encodeHexString(b1);
    }
}
