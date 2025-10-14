import { BrowserRouter, Routes, Route } from "react-router-dom";
import WelcomePage from "./pages/WelcomePage";
import MainWorkPage from "./pages/MainWorkPage";


export default function App() {
    return (
        <BrowserRouter>
            <Routes>
                <Route path="/" element={<WelcomePage/>}/>
                <Route path="/about" element={<MainWorkPage/>}/>
            </Routes>
        </BrowserRouter>
    );
}