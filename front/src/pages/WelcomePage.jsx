import { useNavigate } from "react-router-dom";
import "./WelcomePage.css";

export default function WelcomePage() {
  const navigate = useNavigate();

  return (
    <main className="container">
      <section className="card">
        <h1 className="title">Big Broser is Here.</h1>
        <p className="subtitle">
          BB is the latest service for storing and tracking data about different cities in your MetaVerse.
        </p>
        <button className="main-btn" onClick={() => navigate("/cities")}>
          Test right now
        </button>
      </section>

      <section className="visual-card">
        <img src="/images/city.jpg" alt="City" className="city-img" />
      </section>
    </main>
  );
}
