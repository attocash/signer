FROM scratch

COPY ./build/native/nativeCompile/signer /app/signer

WORKDIR /tmp
WORKDIR /app

EXPOSE 8080

ENTRYPOINT ["./signer"]
