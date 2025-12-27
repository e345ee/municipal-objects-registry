import { HashRouter, Routes, Route } from "react-router-dom";
import WelcomePage from "./pages/WelcomePage";
import CitiesPage from "./pages/CitiesPage";
import CoordinatesPage from "./pages/CoordinatesPage";
import HumansPage from "./pages/HumansPage";
import FeaturePage from "./pages/FeaturePage";
import AppHeader from "./components/AppHeader";

export default function App() {
  return (
    <HashRouter>
      <AppHeader />
      <Routes>
        <Route path="/" element={<WelcomePage />} />
        <Route path="/cities" element={<CitiesPage />} />
        <Route path="/coordinates" element={<CoordinatesPage />} />
        <Route path="/humans" element={<HumansPage />} />
        <Route path="/features" element={<FeaturePage />} />
      </Routes>
    </HashRouter>
  );
}