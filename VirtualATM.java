import java.util.*;
import java.text.SimpleDateFormat;

/* ----------------------------
   Account (abstract base class)
   ---------------------------- */
abstract class Account {
    private final String accHolderName;
    protected String accountNumber;
    protected double balance;
    protected TransactionHistory history;

    public Account(String accountNumber,String accHolderName) {
        this.accHolderName = accHolderName;
        this.accountNumber = accountNumber;
        this.balance = 0.0; // default initial balance = 0.0
        this.history = new TransactionHistory();
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public double getBalance() {
        return balance;
    }

    public void deposit(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Deposit amount must be positive");
        balance += amount;
        String tx = String.format("Deposited ₹%.2f, New Balance: ₹%.2f", amount, balance);
        history.addTransaction(tx);
    }

    public void withdraw(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Withdraw amount must be positive");
        if (amount > balance) throw new IllegalArgumentException("Insufficient funds");
        balance -= amount;
        String tx = String.format("Withdrew ₹%.2f, New Balance: ₹%.2f", amount, balance);
        history.addTransaction(tx);
    }

    public TransactionHistory getHistory() {
        return history;
    }

    public String getAccHolderName(){
        return accHolderName;
    }

    public abstract String getAccountType();
}

/* ----------------------------
   SavingsAccount
   ---------------------------- */
class SavingsAccount extends Account {
    private double interestRate;  // e.g. 0.03 for 3%
    private double minimumBalance;

    public SavingsAccount(String accountNumber,String accHolderName, double interestRate, double minimumBalance) {
        super(accountNumber,accHolderName);
        this.interestRate = interestRate;
        this.minimumBalance = minimumBalance;
    }

    @Override
    public void withdraw(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Withdraw amount must be positive");
        if (balance - amount < minimumBalance) {
            throw new IllegalArgumentException(
                    String.format("Withdrawal would violate minimum balance of ₹%.2f", minimumBalance));
        }
        super.withdraw(amount);
    }

    public void applyInterest() {
        if (balance <= 0) {
            history.addTransaction("Interest applied but balance is zero or negative — nothing added.");
            return;
        }
        double interest = balance * interestRate;
        deposit(interest); // deposit handles history entry
        history.addTransaction(String.format("Interest applied at rate %.2f%%: ₹%.2f", interestRate * 100, interest));
    }

    @Override
    public String getAccountType() {
        return "Savings";
    }
}

/* ----------------------------
   CurrentAccount
   ---------------------------- */
class CurrentAccount extends Account {
    private double minimumBalance;
    private double penaltyFee;

    public CurrentAccount(String accountNumber,String accHolderName, double minimumBalance, double penaltyFee) {
        super(accountNumber,accHolderName);
        this.minimumBalance = minimumBalance;
        this.penaltyFee = penaltyFee;
    }

    @Override
    public void withdraw(double amount) {
        if (amount <= 0) throw new IllegalArgumentException("Withdraw amount must be positive");
        if (amount > balance) throw new IllegalArgumentException("Insufficient funds");
        super.withdraw(amount); // subtract & record

        if (balance < minimumBalance) {
            balance -= penaltyFee;
            String penTx = String.format("Below min balance. Penalty of ₹%.2f applied. New Balance: ₹%.2f", penaltyFee, balance);
            history.addTransaction(penTx);
            System.out.println("Penalty applied: ₹" + String.format("%.2f", penaltyFee));
        }
    }

    @Override
    public String getAccountType() {
        return "Current";
    }
}

/* ----------------------------
   TransactionHistory
   ---------------------------- */
class TransactionHistory {
    private final List<String> transactions = new ArrayList<>();

    public void addTransaction(String transaction) {
        String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
        transactions.add(timestamp + " → " + transaction);
    }

    public void printMiniStatement() {
        System.out.println("----- Mini Statement -----");
        if (transactions.isEmpty()) {
            System.out.println("No transactions yet.");
        } else {
            for (String t : transactions) System.out.println(t);
        }
        System.out.println("--------------------------");
    }
}

/* ----------------------------
   Bank (stores accounts & PINs)
   ---------------------------- */
class Bank {
    private final Map<Long, Account> accounts = new HashMap<>();
    private final Map<Long, Integer> pinMap = new HashMap<>();

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

/* ----------------------------
   VirtualATM (UI + main logic)
   ---------------------------- */
public class VirtualATM {
    private final Bank bank = new Bank();
    private final Scanner input = new Scanner(System.in);

    public VirtualATM() {
        setupDummyUsers(); // optional; you can remove if you want no pre-created users
    }

    private void setupDummyUsers() {
        // Create two demo users with balance 0.0
        bank.addUser(123456789012L, 1234, new SavingsAccount("SA123456789012", "Alex",0.03, 1000.0));
        bank.addUser(7647583948787L, 1231, new CurrentAccount("CA7647583948787", "Joe",5000.0, 50.0));
    }

    private void createNewUserAndMaybeLogin() {
        String AccHolderName;
        System.out.println("Enter Account Holder Name : ");
        AccHolderName = input.next();

        System.out.print("Enter new Account number (digits only): ");
        long accnum=-1L;
        try {
            accnum = Long.parseLong(input.next());
        } catch (NumberFormatException e) {
            System.out.println("Invalid account number. Must be digits only.");
            return;
        }

        if (bank.hasAccount(accnum)) {
            System.out.println("Account already exists. Choose login from main menu.");
            return;
        }

        System.out.print("Set a 4-digit PIN: ");
        String pinStr = input.next();
        if (!isValidPinString(pinStr)) {
            System.out.println("Invalid PIN format. PIN must be exactly 4 digits.");
            return;
        }
        System.out.print("Confirm your 4-digit PIN: ");
        String confirm = input.next();
        if (!pinStr.equals(confirm)) {
            System.out.println("PINs do not match. Aborting account creation.");
            return;
        }
        int pin = Integer.parseInt(pinStr);

        System.out.println("Choose Account Type:");
        System.out.println("1. Savings Account (min balance ₹1000)");
        System.out.println("2. Current Account (min balance ₹5000, penalty ₹50)");
        System.out.print("Enter choice (1 or 2): ");
        int type;
        try {
            type = Integer.parseInt(input.next());
        } catch (NumberFormatException e) {
            System.out.println("Invalid choice. Aborting.");
            return;
        }

        Account account;
        if (type == 1) {
            account = new SavingsAccount("SA" + accnum,AccHolderName ,0.03, 1000.0);
        } else if (type == 2) {
            account = new CurrentAccount("CA" + accnum, AccHolderName,5000.0, 50.0);
        } else {
            System.out.println("Invalid type selected. Defaulting to Savings Account.");
            account = new SavingsAccount("SA" + accnum, AccHolderName,0.03, 1000.0);
        }

        bank.addUser(accnum, pin, account);
        System.out.println("Account created successfully.");

        System.out.print("Do you want to login to the new account now? (yes/no): ");
        String ans = input.next();
        if (ans.equalsIgnoreCase("yes")) {
            if (bank.validatePin(accnum, pin)) {
                atmMenu(bank.getAccount(accnum));
            } else {
                System.out.println("Unexpected error validating PIN after creation.");
            }
        }
    }

    private boolean isValidPinString(String s) {
        return s != null && s.matches("\\d{4}");
    }

    public void run() {
        System.out.println("==== Welcome to Virtual ATM ====");
        while (true) {
            System.out.print("\nEnter your Account Number (or type 'new' to create account, 'exit' to quit): ");
            String token = input.next();

            if (token.equalsIgnoreCase("exit")) {
                System.out.println("Goodbye!");
                break;
            } else if (token.equalsIgnoreCase("new")) {
                createNewUserAndMaybeLogin();
                continue;
            }

            long accNumber;
            try {
                accNumber = Long.parseLong(token);
            } catch (NumberFormatException e) {
                System.out.println("Invalid account number input.");
                continue;
            }

            if (!bank.hasAccount(accNumber)) {
                System.out.println("Account not found. Type 'new' to create account or try again.");
                continue;
            }

            System.out.print("Enter your 4-digit PIN: ");
            String pinStr = input.next();
            if (!isValidPinString(pinStr)) {
                System.out.println("PIN must be exactly 4 digits. Try again.");
                continue;
            }
            int pin = Integer.parseInt(pinStr);

            if (!bank.validatePin(accNumber, pin)) {
                System.out.println("Invalid PIN. Try again.");
                continue;
            }

            System.out.println("Login successful.");
            Account userAccount = bank.getAccount(accNumber);
            atmMenu(userAccount);
        }

        input.close();
    }

    private void atmMenu(Account account) {
        while (true) {
            System.out.println("\n--- ATM Menu (" + account.getAccountType() + " - " + account.getAccountNumber() + " - " +account.getAccHolderName() +") ---");
            System.out.println("1. Check Balance");
            System.out.println("2. Deposit");
            System.out.println("3. Withdraw");
            System.out.println("4. Print Receipt");
            System.out.println("5. Mini Statement");
            System.out.println("6. Apply Interest (Savings only)");
            System.out.println("7. Create New User");
            System.out.println("8. Logout to main screen");
            System.out.print("Enter your choice: ");

            String choiceToken = input.next();
            int choice;
            try {
                choice = Integer.parseInt(choiceToken);
            } catch (NumberFormatException e) {
                System.out.println("Invalid choice. Please enter a number between 1 and 8.");
                continue;
            }

            switch (choice) {
                case 1:
                    System.out.println("Current Balance: ₹" + String.format("%.2f", account.getBalance()));
                    break;
                case 2:
                    System.out.print("Enter amount to deposit: ");
                    try {
                        double amt = Double.parseDouble(input.next());
                        account.deposit(amt);
                        System.out.println("Successfully deposited ₹" + String.format("%.2f", amt));
                        System.out.println("New Balance: ₹" + String.format("%.2f", account.getBalance()));
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid amount. Please enter a valid number.");
                    } catch (IllegalArgumentException iae) {
                        System.out.println(iae.getMessage());
                    }
                    break;
                case 3:
                    System.out.print("Enter amount to withdraw: ");
                    try {
                        double amt = Double.parseDouble(input.next());
                        account.withdraw(amt);
                        System.out.println("Withdrawn ₹" + String.format("%.2f", amt));
                        System.out.println("New Balance: ₹" + String.format("%.2f", account.getBalance()));
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid amount. Please enter a valid number.");
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
                    createNewUserAndMaybeLogin();
                    break;
                case 8:
                    System.out.println("Logging out to main screen.");
                    return; // return to main run loop
                default:
                    System.out.println("Invalid choice. Please choose 1–8.");
            }
        }
    }

    private void printReceipt(Account account) {
        System.out.println("----- ATM Receipt -----");
        System.out.println("Account Holder Name : "+account.getAccHolderName());
        System.out.println("Account Number: " + account.getAccountNumber());
        System.out.println("Account Type: " + account.getAccountType());
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
