import { Chart, registerables } from 'chart.js';

Chart.register(...registerables);

let charts = [];

function destroyCharts() {
    charts.forEach((chart) => chart.destroy());
    charts = [];
}

function getCanvas(id) {
    return document.getElementById(id);
}

function initArticleDashboardCharts() {
    const dataElement = document.getElementById('article-dashboard-chart-data');

    if (!dataElement) {
        return;
    }

    destroyCharts();

    const data = JSON.parse(dataElement.textContent);
    const chartConfig = {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
            legend: {
                labels: {
                    color: '#7e739a',
                    font: {
                        size: 14,
                    },
                },
            },
        },
    };

    charts.push(new Chart(getCanvas('statusPie'), {
        type: 'doughnut',
        data: {
            labels: ['VISIBLE', 'MASQUE'],
            datasets: [{
                data: [data.statusVisible, data.statusMasque],
                backgroundColor: ['#7c3aed', '#f472b6'],
                borderColor: '#fff',
                borderWidth: 2,
            }],
        },
        options: chartConfig,
    }));

    charts.push(new Chart(getCanvas('favorisBar'), {
        type: 'bar',
        data: {
            labels: data.labels,
            datasets: [{
                label: 'Favoris',
                data: data.favorisData,
                backgroundColor: '#fbbf24',
                borderRadius: 8,
            }],
        },
        options: chartConfig,
    }));

    charts.push(new Chart(getCanvas('commentairesBar'), {
        type: 'line',
        data: {
            labels: data.labels,
            datasets: [{
                label: 'Commentaires',
                data: data.commentairesData,
                borderColor: '#7c3aed',
                backgroundColor: 'rgba(124,58,237,.1)',
                tension: 0.35,
                fill: true,
                borderWidth: 2,
                pointBackgroundColor: '#7c3aed',
                pointRadius: 4,
            }],
        },
        options: chartConfig,
    }));

    charts.push(new Chart(getCanvas('activityByDate'), {
        type: 'line',
        data: {
            labels: data.activityLabels,
            datasets: [{
                label: 'Articles ajoutés',
                data: data.articlesByDay,
                borderColor: 'rgba(147, 51, 234, 1)',
                backgroundColor: 'rgba(147, 51, 234, 0.2)',
                fill: true,
            }, {
                label: 'Favoris ajoutés',
                data: data.favorisByDay,
                borderColor: 'rgba(59, 130, 246, 1)',
                backgroundColor: 'rgba(59, 130, 246, 0.2)',
                fill: true,
            }, {
                label: 'Commentaires ajoutés',
                data: data.commentairesByDay,
                borderColor: 'rgba(251, 191, 36, 1)',
                backgroundColor: 'rgba(251, 191, 36, 0.2)',
                fill: true,
            }],
        },
        options: {
            responsive: true,
            plugins: {
                legend: {
                    position: 'top',
                },
            },
        },
    }));
}

document.addEventListener('turbo:load', initArticleDashboardCharts);
document.addEventListener('DOMContentLoaded', initArticleDashboardCharts);
document.addEventListener('turbo:before-cache', destroyCharts);
