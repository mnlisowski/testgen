package sample.fit;

public class FraudSignal {
    private final CustomerProfile customer;
    private final PaymentAttempt payment;
    private final DeviceProfile device;
    private final AddressProfile address;
    private final String ipCountry;
    private final boolean vpnDetected;

    public FraudSignal(
            CustomerProfile customer,
            PaymentAttempt payment,
            DeviceProfile device,
            AddressProfile address,
            String ipCountry,
            boolean vpnDetected
    ) {
        this.customer = customer;
        this.payment = payment;
        this.device = device;
        this.address = address;
        this.ipCountry = ipCountry;
        this.vpnDetected = vpnDetected;
    }

    public CustomerProfile getCustomer() {
        return customer;
    }

    public PaymentAttempt getPayment() {
        return payment;
    }

    public DeviceProfile getDevice() {
        return device;
    }

    public AddressProfile getAddress() {
        return address;
    }

    public String getIpCountry() {
        return ipCountry;
    }

    public boolean isVpnDetected() {
        return vpnDetected;
    }

    public boolean countryMismatch() {
        if (customer == null || address == null) {
            return true;
        }
        if (ipCountry == null || ipCountry.isBlank()) {
            return true;
        }
        if (!customer.isCountry(ipCountry)) {
            return true;
        }
        return !customer.isCountry(address.getCountryCode());
    }

    public boolean hasSuspiciousShape() {
        if (payment == null || device == null) {
            return true;
        }
        if (vpnDetected || device.isVpnDetected()) {
            return true;
        }
        if (payment.getAmount() > 1000 && payment.getMethod() == PaymentMethod.CRYPTO) {
            return true;
        }
        if (device.getFailedLogins() > 2 && payment.getFailureCount() > 0) {
            return true;
        }
        return countryMismatch();
    }

    public int combinedRisk() {
        int risk = 0;
        if (customer != null) {
            risk += customer.getRiskScore();
        } else {
            risk += 40;
        }
        if (payment != null) {
            risk += payment.paymentRisk();
        } else {
            risk += 40;
        }
        if (device != null) {
            risk += device.riskPoints();
        }
        if (address != null) {
            risk += address.addressRiskPoints();
        }
        if (hasSuspiciousShape()) {
            risk += 25;
        }
        if (risk < 0) {
            return 0;
        }
        return risk;
    }
}
