package cn.hyperchain.sdk.provider;

import cn.hyperchain.sdk.exception.RequestException;
import cn.hyperchain.sdk.exception.RequestExceptionCode;
import cn.hyperchain.sdk.grpc.Transaction.CommonReq;
import cn.hyperchain.sdk.grpc.Transaction.CommonRes;
import cn.hyperchain.sdk.grpc.GrpcUtil;
import io.grpc.stub.StreamObserver;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class StreamManager extends Manager {
    private static Logger logger = LogManager.getLogger(StreamManager.class);

    private StreamObserver<CommonReq> reqStreamObserver;
    private StreamObserver<CommonRes> resStreamObserver;
    private CountDownLatch finishLatch;
    private CommonRes response;
    private Throwable error;
    private boolean isNormal;
    private boolean isUsed;

    public void setUsed(boolean used) {
        isUsed = used;
    }

    public boolean isUsed() {
        return isUsed;
    }

    public StreamManager(String method, GrpcProvider provider) throws RequestException {
        super(method, provider);
        this.generateFromMethod();
    }

    public StreamObserver<CommonReq> getReqStreamObserver() {
        return reqStreamObserver;
    }

    public StreamObserver<CommonRes> getResStreamObserver() {
        return resStreamObserver;
    }

    private void setReqStreamObserver(StreamObserver<CommonReq> reqStreamObserver) {
        this.reqStreamObserver = reqStreamObserver;
    }

    private void setResStreamObserver(StreamObserver<CommonRes> resStreamObserver) {
        this.resStreamObserver = resStreamObserver;
    }

    private void setNormal(boolean isNormal) {
        this.isNormal = isNormal;
    }

    private void setGrpcProvider(GrpcProvider grpcProvider) {
        this.grpcProvider = grpcProvider;
    }

    private void generateFromMethod() throws RequestException {
        finishLatch = new CountDownLatch(1);
        StreamObserver<CommonRes> resStreamObserver = new StreamObserver<CommonRes>() {
            @Override
            public void onNext(CommonRes commonRes) {
                response = commonRes;
                error = null;
                finishLatch.countDown();
            }

            @Override
            public void onError(Throwable throwable) {
                if (throwable.getMessage().equals("ABORTED: stream idle timeout")) {
                    logger.warn("GRPC Stream with the node " + grpcProvider.getUrl() + " failed. The reason is " + throwable.getMessage());
                } else {
                    logger.error("GRPC Stream with the node " + grpcProvider.getUrl() + " failed. The reason is " + throwable.getMessage());
                }
                finishLatch.countDown();
                setNormal(false);
                error = throwable;
            }

            @Override
            public void onCompleted() {
                logger.debug("GRPC Stream with the node " + grpcProvider.getUrl() + " closed.");
                finishLatch.countDown();
                error = null;
                setNormal(false);
            }
        };
        StreamObserver<CommonReq> reqStreamObserver = GrpcUtil.getReqByMethod(method, grpcProvider.getChannel(), resStreamObserver);
        this.setReqStreamObserver(reqStreamObserver);
        this.setResStreamObserver(resStreamObserver);
        this.setNormal(true);
        this.setGrpcProvider(grpcProvider);
    }


    public boolean isNormal() {
        return isNormal;
    }

    /**
     * grpc stream send request and get response.
     * @param commonReq request
     * @return Commonres
     * @throws RequestException -
     */
    @Override
    public CommonRes onNext(CommonReq commonReq) throws RequestException {
        if (reqStreamObserver != null) {
            reqStreamObserver.onNext(commonReq);
        }
        try {
            boolean isok = finishLatch.await(grpcProvider.getConnectTimeout(), TimeUnit.MILLISECONDS);
            finishLatch = new CountDownLatch(1);
            if (error != null) {
                throw new RequestException(RequestExceptionCode.GRPC_STREAM_FAILED, error.getMessage());
            }
            if (!isok) {
                throw new RequestException(RequestExceptionCode.GRPC_STREAM_FAILED, "grpc request time out, more than " + grpcProvider.getConnectTimeout() + " milliseconds");
            }
            return response;
        } catch (InterruptedException e) {
            setNormal(false);
            throw new RequestException(RequestExceptionCode.GRPC_STREAM_FAILED, e.getMessage());
        }
    }
}
