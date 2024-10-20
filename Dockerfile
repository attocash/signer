FROM scratch

COPY ./build/native/nativeCompile/signer /app/signer

WORKDIR /app

EXPOSE 8080

ENTRYPOINT ["./signer"]
