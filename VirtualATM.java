import java.util.*;
import java.text.SimpleDateFormat;

// Base account class
abstract class Account {
    protected String accountNumber;
    // Using BigDecimal (or long for cents) is better in real systems, but for simplicity using double
    protected double balance;
    protected TransactionHistory history;

    public Account(String accountNumber, double initialBalance) {
        this.accountNumber = accountNumber;
        this.balance = initialBalance;
        this.history = new TransactionHistory();
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public double getBalance() {
        return balance;
    }

    public void deposit(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Deposit amount must be positive");
        }
        balance += amount;
        String tx = String.format("Deposited ₹%.2f, New Balance: ₹%.2f", amount, balance);
        history.addTransaction(tx);
    }

    public void withdraw(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Withdraw amount must be positive");
        }
        if (amount > balance) {
            throw new IllegalArgumentException("Insufficient funds");
        }
        balance -= amount;
        String tx = String.format("Withdrew ₹%.2f, New Balance: ₹%.2f", amount, balance);
        history.addTransaction(tx);
    }

    public TransactionHistory getHistory() {
        return history;
    }
}

// Savings account with interest & minimum balance requirement
class SavingsAccount extends Account {
    private double interestRate;  // e.g. 0.03 for 3%
    private double minimumBalance;

    public SavingsAccount(String accountNumber, double initialBalance,
                          double interestRate, double minimumBalance) {
        super(accountNumber, initialBalance);
        this.interestRate = interestRate;
        this.minimumBalance = minimumBalance;
    }

    @Override
    public void withdraw(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Withdraw amount must be positive");
        }
        if (balance - amount < minimumBalance) {
            throw new IllegalArgumentException(
                    String.format("Withdrawal would violate minimum balance of ₹%.2f", minimumBalance));
        }
        super.withdraw(amount);
    }

    public void applyInterest() {
        double interest = balance * interestRate;
        // deposit will add to history
        deposit(interest);
        history.addTransaction(
                String.format("Interest applied at rate %.2f%%: ₹%.2f", interestRate * 100, interest)
        );
    }
}

// Current (checking) account with possible penalty for dropping below minimum
class CurrentAccount extends Account {
    private double minimumBalance;
    private double penaltyFee;

    public CurrentAccount(String accountNumber, double initialBalance,
                          double minimumBalance, double penaltyFee) {
        super(accountNumber, initialBalance);
        this.minimumBalance = minimumBalance;
        this.penaltyFee = penaltyFee;
    }

    @Override
    public void withdraw(double amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("Withdraw amount must be positive");
        }
        if (amount > balance) {
            throw new IllegalArgumentException("Insufficient funds");
        }
        balance -= amount;
        String tx = String.format("Withdrew ₹%.2f, New Balance: ₹%.2f", amount, balance);
        history.addTransaction(tx);

        if (balance < minimumBalance) {
            balance -= penaltyFee;
            String penTx = String.format(
                    "Below min balance. Penalty of ₹%.2f applied. New Balance: ₹%.2f",
                    penaltyFee, balance
            );
            history.addTransaction(penTx);
            System.out.println("Penalty applied: ₹" + penaltyFee);
        }
    }
}

// Class to store user metadata (pin, mobile, etc.)
class UserData {
    private long accNumber;
    private long mobileNumber;
    private int pin;

    public UserData(long accNumber, long mobileNumber, int pin) {
        this.accNumber = accNumber;
        this.mobileNumber = mobileNumber;
        this.pin = pin;
    }

    public long getAccNumber() {
        return accNumber;
    }
    public int getPin() {
        return pin;
    }
    public long getMobileNumber() {
        return mobileNumber;
    }
}

// Transaction history wrapper
class TransactionHistory {
    private List<String> transactions = new ArrayList<>();

    public void addTransaction(String transaction) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        transactions.add(timestamp + " → " + transaction);
    }

    public void printMiniStatement() {
        System.out.println("----- Mini Statement -----");
        for (String t : transactions) {
            System.out.println(t);
        }
        System.out.println("--------------------------");
    }
}

// Bank class to maintain accounts and user login
class Bank {
    private Map<Long, Account> accounts = new HashMap<>();
    private Map<Long, Integer> pinMap = new HashMap<>();

    public void addUser(long accNum, int pin, Account account) {
        accounts.put(accNum, account);
        pinMap.put(accNum, pin);
    }

    public boolean validatePin(long accNum, int pin) {
        return pinMap.containsKey(accNum) && pinMap.get(accNum) == pin;
    }

    public Account getAccount(long accNum) {
        return accounts.get(accNum);
    }

    public boolean hasAccount(long accNum) {
        return accounts.containsKey(accNum);
    }
}

// ATM / UI class
public class VirtualATM {
    private Bank bank;
    private Scanner input;

    public VirtualATM() {
        bank = new Bank();
        input = new Scanner(System.in);
        setupDummyUsers();
    }

    private void setupDummyUsers() {
        // Add some users. In real scenario, this would come from DB or file.
        // Here's example data:
        bank.addUser(342351231212L, 1234,
                new SavingsAccount("SA342351231212", 5000.0, 0.03, 1000.0));

        bank.addUser(7647583948787L, 1231,
                new CurrentAccount("CA7647583948787", 10000.0, 5000.0, 50.0));
        // Add more as needed
    }

    public void run() {
        System.out.print("Enter your Account Number: ");
        long accNumber = -1L;
        try {
            accNumber = input.nextLong();
        } catch (InputMismatchException ime) {
            System.out.println("Invalid account number input.");
            return;
        }

        if (!bank.hasAccount(accNumber)) {
            System.out.println("Account not found. Exiting.");
            return;
        }

        System.out.print("Enter your 4-digit PIN: ");
        String pinStr = input.next();  // read as string for better control
        if (pinStr.length() != 4 || !pinStr.matches("\\d{4}")) {
            System.out.println("PIN must be exactly 4 digits. Exiting.");
            return;
        }
        int pin = Integer.parseInt(pinStr);

        if (!bank.validatePin(accNumber, pin)) {
            System.out.println("Invalid PIN. Exiting.");
            return;
        }

        System.out.println("Login successful.");

        Account userAccount = bank.getAccount(accNumber);
        atmMenu(userAccount);
    }

    private void atmMenu(Account account) {
        String ch="yes";
        while (ch.equalsIgnoreCase("yes")) {
            System.out.println("\n--- ATM Menu ---");
            System.out.println("1. Check Balance");
            System.out.println("2. Deposit");
            System.out.println("3. Withdraw");
            System.out.println("4. Print Receipt");
            System.out.println("5. Mini Statement");
            System.out.println("6. Apply Interest (Savings only)");
            System.out.println("7. Exit");
            System.out.print("Enter your choice: ");

            int choice;
            try {
                choice = input.nextInt();
            } catch (InputMismatchException ime) {
                input.nextLine();  // consume invalid input
                System.out.println("Invalid input. Please enter a number between 1 and 7.");
                continue;
            }

            switch (choice) {
                case 1:
                    System.out.println("Current Balance: ₹" + String.format("%.2f", account.getBalance()));
                    break;
                case 2:
                    System.out.print("Enter amount to deposit: ");
                    try {
                        double amt = input.nextDouble();
                        account.deposit(amt);
                        System.out.println("Successfully deposited ₹" + String.format("%.2f", amt));
                        System.out.println("New Balance: ₹" + String.format("%.2f", account.getBalance()));
                    } catch (InputMismatchException ime) {
                        input.nextLine();
                        System.out.println("Invalid input. Please enter a valid number.");
                    } catch (IllegalArgumentException iae) {
                        System.out.println(iae.getMessage());
                    }
                    break;
                case 3:
                    System.out.print("Enter amount to withdraw: ");
                    try {
                        double amt = input.nextDouble();
                        account.withdraw(amt);
                        System.out.println("Withdrawn ₹" + String.format("%.2f", amt));
                        System.out.println("New Balance: ₹" + String.format("%.2f", account.getBalance()));
                    } catch (InputMismatchException ime) {
                        input.nextLine();
                        System.out.println("Invalid input. Please enter a valid number.");
                    } catch (IllegalArgumentException iae) {
                        System.out.println(iae.getMessage());
                    }
                    break;
                case 4:
                    printReceipt(account);
                    break;
                case 5:
                    account.getHistory().printMiniStatement();
                    break;
                case 6:
                    if (account instanceof SavingsAccount) {
                        ((SavingsAccount) account).applyInterest();
                        System.out.println("Interest applied.");
                    } else {
                        System.out.println("Not a Savings Account. Cannot apply interest.");
                    }
                    break;
                case 7:
                    System.out.println("Thank you for using the ATM. Goodbye!");
                    return;
                default:
                    System.out.println("Invalid choice. Please choose 1–7.");
            }
            System.out.print("\nDo you want to go back (YES/NO): ");
            input.nextLine(); // clear newline from buffer
            ch = input.nextLine();

            if (!ch.equalsIgnoreCase("yes")) {
                System.out.println("Thank you for using the ATM. Goodbye!");
                System.exit(0);
            }

        }
    }

    private void printReceipt(Account account) {
        System.out.println("----- ATM Receipt -----");
        System.out.println("Account Number: " + account.getAccountNumber());
        System.out.println("Account Type: " + (account instanceof SavingsAccount ? "Savings" : "Current"));
        String now = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        System.out.println("Date/Time: " + now);
        System.out.println("Available Balance: ₹" + String.format("%.2f", account.getBalance()));
        System.out.println("------------------------");
    }

    public static void main(String[] args) {
        VirtualATM atm = new VirtualATM();
        atm.run();
    }
}