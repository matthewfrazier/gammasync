# GitHub Issues for iOS Business Setup

Create these GitHub issues to track business setup progress. Copy each issue template and create manually in the repository.

## Issue 1: LLC Formation and Business Entity Setup

**Title**: `Business Setup: Form Delaware LLC for GammaSync iOS`

**Priority**: High
**Assignee**: @claude (for research and guidance)
**Estimated Time**: 1-2 weeks

**Description**:
```
## Objective
Establish Delaware LLC for GammaSync iOS app development and App Store distribution.

## Tasks
- [ ] Choose and verify LLC name availability
- [ ] File Delaware Certificate of Formation ($90)
- [ ] Set up registered agent service ($100-300/year)
- [ ] Obtain Federal EIN (free at irs.gov)
- [ ] Draft basic operating agreement
- [ ] Open business bank account

## Deliverables
- [ ] Filed Certificate of Formation
- [ ] Federal EIN documentation
- [ ] Operating agreement (signed)
- [ ] Business bank account (opened)

## Dependencies
None

## Automation Opportunities
- Monitor state filing status via Delaware corp.delaware.gov portal
- Use online EIN application (immediate results)
- Digital operating agreement signing via DocuSign

## Cost Estimate
- **DIY**: $290-490 first year
- **Professional**: $890-1,390 first year

## Timeline
1-2 weeks (can expedite to 3-5 days if needed)

## Notes
Delaware recommended for app businesses despite higher costs. Required for organization-level Apple Developer account.
```

---

## Issue 2: D&B DUNS Number Application

**Title**: `Business Setup: Obtain D&B DUNS Number for Apple Developer`

**Priority**: High
**Assignee**: @claude (for guidance and verification)
**Estimated Time**: 1-2 weeks

**Description**:
```
## Objective
Obtain D&B DUNS number for LLC to enable Apple Developer Organization account.

## Tasks
- [ ] Verify business doesn't already exist in D&B database
- [ ] Complete DUNS application at dnb.com/duns-number/lookup
- [ ] Provide required information:
  - [ ] Legal business name (exact match to LLC)
  - [ ] Physical business address (not PO box)
  - [ ] Business phone number
  - [ ] Federal EIN
  - [ ] Primary business activity (Software Development)
- [ ] Monitor application status
- [ ] Verify DUNS number activation

## Deliverables
- [ ] Active D&B DUNS number
- [ ] DUNS verification documentation

## Dependencies
- Requires: LLC formation completed
- Requires: Federal EIN obtained

## Automation Opportunities
- Set up D&B monitoring for business credit tracking
- Automated status checking via D&B API (if available)

## Cost Estimate
- **Standard**: Free (5-10 business days)
- **Expedited**: $500 (1-2 business days)

## Timeline
1-2 weeks for standard processing

## Notes
Required for Apple Developer Organization account. Cannot proceed with App Store setup without DUNS number.
```

---

## Issue 3: Apple Developer Organization Account

**Title**: `Business Setup: Register Apple Developer Organization Account`

**Priority**: High
**Assignee**: @claude (for setup guidance)
**Estimated Time**: 1 week

**Description**:
```
## Objective
Set up Apple Developer Organization account for GammaSync iOS app distribution.

## Tasks
- [ ] Gather required documentation:
  - [ ] D&B DUNS number
  - [ ] LLC formation documents
  - [ ] Federal EIN documentation
  - [ ] Authorized signatory identification
- [ ] Complete Apple Developer enrollment
- [ ] Pay annual fee ($99)
- [ ] Verify organization details with Apple
- [ ] Set up App Store Connect access
- [ ] Configure development team roles

## Deliverables
- [ ] Active Apple Developer Organization account
- [ ] App Store Connect access
- [ ] Development certificates
- [ ] Team member access configured

## Dependencies
- Requires: D&B DUNS number (Issue #2)
- Requires: LLC formation (Issue #1)

## Automation Opportunities
- Use Apple Developer API for:
  - Certificate management
  - App metadata updates
  - TestFlight distribution
- Automated renewal reminders

## Cost Estimate
$99/year

## Timeline
1-3 business days after DUNS verification

## Notes
Organization account required for business app distribution. Individual accounts cannot establish business credibility.
```

---

## Issue 4: Medical App Legal Compliance Review

**Title**: `Legal: Medical App Compliance Review for FDA/Health Claims`

**Priority**: High
**Assignee**: @claude (research) + Legal counsel
**Estimated Time**: 2-4 weeks

**Description**:
```
## Objective
Ensure GammaSync iOS app complies with FDA regulations and App Store medical app guidelines.

## Tasks
- [ ] Research FDA medical device regulations for "gamma entrainment"
- [ ] Review App Store medical app guidelines
- [ ] Analyze therapeutic claims in current Android app
- [ ] Draft FDA-compliant disclaimers and warnings
- [ ] Create privacy policy for health data
- [ ] Review marketing claims and positioning
- [ ] Determine classification (wellness vs medical device)

## Deliverables
- [ ] Legal compliance assessment
- [ ] FDA disclaimer text
- [ ] Privacy policy (health data focus)
- [ ] Marketing guidelines document
- [ ] App Store review strategy

## Dependencies
None (can start immediately)

## Automation Opportunities
- Automated compliance monitoring
- Policy template generation
- Disclaimer validation

## Cost Estimate
- **Basic Review**: $1,000-2,000
- **Comprehensive**: $3,000-5,000

## Timeline
2-4 weeks (depending on complexity)

## Notes
Critical for App Store approval. Medical/therapeutic apps face additional scrutiny. Early review recommended.
```

---

## Issue 5: iOS Development Environment Setup

**Title**: `Development: Set up iOS development environment and tools`

**Priority**: Medium
**Assignee**: @claude (automation and guidance)
**Estimated Time**: 1 week

**Description**:
```
## Objective
Establish complete iOS development environment for GammaSync iOS app.

## Hardware Requirements
- [ ] Mac computer (macOS 13.0+)
- [ ] iOS test device (iPhone 13 Pro+ with 120Hz ProMotion)
- [ ] External display for XREAL testing (optional)

## Software Setup Tasks
- [ ] Install Xcode 15.0+
- [ ] Install iOS SDK 17.0+
- [ ] Set up Kotlin Multiplatform Mobile plugin
- [ ] Configure Swift Package Manager / CocoaPods
- [ ] Install development certificates
- [ ] Set up iOS Simulator
- [ ] Configure Instruments for profiling
- [ ] Set up TestFlight access

## Project Structure Setup
- [ ] Create KMP shared module
- [ ] Migrate Android domain layer to shared
- [ ] Create iOS Xcode project
- [ ] Configure build systems
- [ ] Set up CI/CD pipeline

## Deliverables
- [ ] Functional iOS development environment
- [ ] KMP project structure
- [ ] iOS project template
- [ ] Build and test automation

## Dependencies
- Requires: Apple Developer account (Issue #3)

## Automation Opportunities
- Automated Xcode/SDK updates
- CI/CD pipeline with GitHub Actions
- Automated certificate management
- Build script automation

## Cost Estimate
- **Hardware** (if needed): $2,000-5,000
- **Software**: $99/year (included in Apple Developer)

## Timeline
1 week for setup, ongoing for optimization

## Notes
ProMotion device required for accurate 120Hz testing. Cannot simulate real-time performance.
```

---

## Issue 6: Business Insurance and Risk Management

**Title**: `Business: Obtain business insurance for medical app liability`

**Priority**: Medium
**Assignee**: @claude (research) + Insurance broker
**Estimated Time**: 2-3 weeks

**Description**:
```
## Objective
Secure appropriate business insurance coverage for medical/therapeutic app development and distribution.

## Insurance Types Needed
- [ ] Professional Liability (E&O) Insurance
  - Coverage for software errors/omissions
  - Medical app specific coverage
- [ ] General Liability Insurance
  - Basic business operations protection
- [ ] Cyber Liability Insurance
  - Data breach and privacy protection
  - Critical for health-related data

## Tasks
- [ ] Research insurance providers for tech companies
- [ ] Get quotes for medical app coverage
- [ ] Review policy terms and exclusions
- [ ] Select and purchase policies
- [ ] Set up automatic renewals
- [ ] Document coverage for legal compliance

## Deliverables
- [ ] Active insurance policies
- [ ] Coverage documentation
- [ ] Certificate of insurance
- [ ] Risk management plan

## Dependencies
- Requires: LLC formation (Issue #1)

## Automation Opportunities
- Automated renewal reminders
- Claims tracking system
- Policy comparison tools

## Cost Estimate
- **Professional Liability**: $300-800/year
- **General Liability**: $200-500/year
- **Cyber Liability**: $500-1,500/year
- **Total**: $1,000-2,800/year

## Timeline
2-3 weeks for quotes and setup

## Notes
Medical app insurance more expensive than general software. Cyber liability critical for any health data handling.
```

---

## Issue 7: Financial and Accounting Setup

**Title**: `Business: Set up financial systems and accounting`

**Priority**: Low
**Assignee**: @claude (automation research)
**Estimated Time**: 1-2 weeks

**Description**:
```
## Objective
Establish business financial systems for iOS app development and revenue tracking.

## Tasks
- [ ] Set up business banking account
- [ ] Choose accounting software (QuickBooks vs alternatives)
- [ ] Set up chart of accounts for app business
- [ ] Configure revenue recognition for app sales
- [ ] Set up expense tracking categories
- [ ] Establish bookkeeping procedures
- [ ] Set up tax compliance procedures
- [ ] Configure payment processing for potential B2B sales

## Deliverables
- [ ] Business bank account
- [ ] Accounting software setup
- [ ] Financial procedures documented
- [ ] Tax preparation system
- [ ] Revenue tracking system

## Dependencies
- Requires: LLC formation (Issue #1)
- Requires: Federal EIN (Issue #1)

## Automation Opportunities
- Bank transaction import to accounting software
- Automated expense categorization
- Revenue recognition automation
- Tax document generation
- Financial reporting automation

## Cost Estimate
- **Business Banking**: $10-50/month
- **Accounting Software**: $15-50/month
- **Bookkeeping**: $200-500/month (if outsourced)

## Timeline
1-2 weeks for initial setup

## Notes
Can start with simple setup and expand as business grows. Focus on automation from the beginning.
```

---

## Summary for Issue Creation

**High Priority (Start Immediately)**:
1. LLC Formation (#1)
2. D&B DUNS Number (#2)
3. Apple Developer Account (#3)
4. Medical App Legal Review (#4)

**Medium Priority (After High Priority)**:
5. iOS Development Environment (#5)
6. Business Insurance (#6)

**Low Priority (Can Defer)**:
7. Financial Systems (#7)

**Total Estimated Timeline**: 4-6 weeks for high priority items
**Total Estimated Cost**: $1,600-4,500 first year

Copy each issue template above and create manually in the GitHub repository, assigning to @claude where indicated.