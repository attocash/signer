Feature: Signature

  Scenario: Block signing should be allowed
    Given signer has BLOCK capability

    When block is signed

    Then signature is valid

  Scenario: Vote signing should be allowed
    Given signer has VOTE capability

    When vote is signed

    Then signature is valid

  Scenario: Challenge signing should be allowed
    Given signer has CHALLENGE capability

    When challenge is signed

    Then signature is valid

  Scenario: Signing should NOT be allowed when capability is not supported
    Given signer has VOTE capability

    When block is not signed
