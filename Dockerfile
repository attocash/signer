FROM scratch

COPY ./build/native/nativeCompile/gatekeeper /app/gatekeeper

WORKDIR /app

EXPOSE 8080

ENTRYPOINT ["./gatekeeper"]
