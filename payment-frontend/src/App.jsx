import { useState } from "react";
import api from "./api";

function App() {
  const [amount, setAmount] = useState("");
  const [loading, setLoading] = useState(false);
  const [message, setMessage] = useState("");

  const loadRazorpayScript = () => {
    return new Promise((resolve) => {
      const script = document.createElement("script");
      script.src = "https://checkout.razorpay.com/v1/checkout.js";
      script.onload = () => {
        resolve(true);
      };
      script.onerror = () => {
        resolve(false);
      };
      document.body.appendChild(script);
    });
  };

  const handlePayment = async () => {
    setMessage("");
    if (!amount || Number(amount) <= 0) {
      setMessage("Please enter a valid amount");
      return;
    }

    try {
      setLoading(true);

      // ------------------------------------------------
      // STEP 1: Create Razorpay Order
      // ------------------------------------------------
      const response = await api.post("/payments/create-order", {
        amount: Math.round(Number(amount)), 
      });
      const order = response.data;

      // ------------------------------------------------
      // STEP 2: Load Razorpay Checkout
      // ------------------------------------------------
      const loaded = await loadRazorpayScript();
      if (!loaded) {
        setMessage("Unable to load Razorpay Checkout");
        setLoading(false);
        return;
      }

      // ------------------------------------------------
      // STEP 3: Razorpay Checkout Options
      // ------------------------------------------------
      const options = {
        key: order.keyId,
        amount: order.amount,
        currency: order.currency,
        name: "My Payment Gateway",
        description: "Test Payment",
        order_id: order.orderId,
        handler: async function (paymentResponse) {
          try {
            // --------------------------------------------
            // STEP 4: Verify Payment
            // --------------------------------------------
            await api.post("/payments/verify", {
              razorpayOrderId: paymentResponse.razorpay_order_id,
              razorpayPaymentId: paymentResponse.razorpay_payment_id,
              razorpaySignature: paymentResponse.razorpay_signature,
            });
            setMessage("Payment Successful!");
          } catch (error) {
            console.error(error);
            setMessage("Payment verification failed");
          }
        },
        prefill: {
          name: "Test Customer",
          email: "test@example.com",
          contact: "9999999999",
        },
        theme: {
          color: "#3399cc",
        },
      };

      const razorpay = new window.Razorpay(options);
      razorpay.on("payment.failed", function (response) {
        console.error(response.error);
        setMessage("Payment Failed");
      });
      razorpay.open();

    } catch (error) {
      console.error("FULL ERROR DETAILS:", error);
      const serverMessage = error.response?.data;
      const genericMessage = error.message || "Unable to create payment";
      setMessage(typeof serverMessage === 'string' ? serverMessage : genericMessage);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div
      style={{
        width: "400px",
        margin: "100px auto",
        textAlign: "center",
      }}
    >
      <h1>Payment Gateway</h1>
      <input
        type="number" id="amount" name="amount"
        placeholder="Enter Amount"
        value={amount}
        onChange={(e) => setAmount(e.target.value)}
        style={{
          width: "100%",
          padding: "12px",
          marginBottom: "20px",
        }}
      />
      <button
        onClick={handlePayment}
        disabled={loading}
        style={{
          width: "100%",
          padding: "12px",
          cursor: "pointer",
        }}
      >
        {loading ? "Processing..." : "PAY NOW"}
      </button>
      {message && <h3>{message}</h3>}
    </div>
  );
}

export default App;
