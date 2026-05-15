package sample;

  public class SwitchDiscountCalculator {

      public enum CustomerType {
          REGULAR,
          SILVER,
          GOLD
      }

      public double calculate(CustomerType customerType) {
          switch (customerType) {
              case GOLD:
                  return 0.15;
              case SILVER:
                  return 0.10;
              case REGULAR:
                  return 0.05;
              default:
                  return 0.0;
          }
      }
  }
