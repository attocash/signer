# signer

The signer uses Commons 10 address-based block and vote models. Requests identify accounts with `address`
and role addresses such as `representativeAddress`, without duplicate public-key or address-algorithm
fields. The `/blocks` and `/votes` request adapters accept legacy identity pairs when the corresponding canonical address is absent; supplied canonical addresses take precedence over duplicate key fields. Hash algorithms such as `sendHashAlgorithm` and
`blockAlgorithm` retain their meaning; protocol bytes and signatures are unchanged.

Upgrade signer before sending canonical requests from upgraded Node or Commons clients. The `/public-keys`
endpoint continues returning the signer's public key, and authentication and signing capabilities are
unchanged. Wallet-server migration and production rollout are separate work.
