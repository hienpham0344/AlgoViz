# Enterprise Workflow: Safe Cloud Infrastructure Deployment (IaC)

## 1. Overview
Executes safe, audited, drift-free infrastructure updates using Terraform, modular architectures, and remote state locking.

## 2. Participating Subagents
- **`cloud_architect`**: Designs and edits Terraform modules.
- **`security_auditor`**: Runs `tfsec` and IAM policy least-privilege checks.
- **`sre_specialist`**: Reviews maintenance windows, rollback strategies, and monitoring.

## 3. Step-by-Step Execution Sequence

### Phase 1: Static Linting & Security Scan
```bash
terraform fmt -check
tflint
tfsec .
```

### Phase 2: State Lock & Plan Generation
```bash
terraform init -backend=true
terraform plan -out=tfplan.binary
terraform show -json tfplan.binary > tfplan.json
```

### Phase 3: Plan Review & Blast Radius Audit
- Verify the exact list of resources to be `created`, `modified`, or `destroyed`.
- **CRITICAL GATE**: Any `destroy` on production databases, VPCs, or DNS zones requires explicit architect sign-off.

### Phase 4: Apply & Drift Verification
```bash
terraform apply tfplan.binary
terraform plan -detailed-exitcode # Verifies 0 drift post-apply
```
