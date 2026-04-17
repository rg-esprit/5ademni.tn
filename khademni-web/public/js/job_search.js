/**
 * Job Search AJAX Handler
 * Handles real-time search, dual-range salary slider, and dynamic updates.
 * Compatible with Symfony Turbo (uses turbo:load instead of DOMContentLoaded)
 */

(function () {
    function initJobSearch() {
        const searchForm = document.getElementById('job-search-form');
        const jobList = document.getElementById('job-list');
        const jobWrapper = document.getElementById('job-list-wrapper');

        // Sliders
        const minSlider = document.getElementById('salary-min');
        const maxSlider = document.getElementById('salary-max');
        const salaryDisplay = document.getElementById('salary-display');
        const track = document.getElementById('slider-track');

        // Inputs for live search
        const queryInput = document.getElementById('sq');
        const catSelect = document.getElementById('scat');
        const typeSelect = document.getElementById('sjt');
        const sortInput = document.getElementById('js-sort');
        const pageSortSelect = document.getElementById('sort');

        if (!searchForm || !jobList || !minSlider || !maxSlider) return;
        // Prevent double-binding if already initialized
        if (searchForm.dataset.searchBound) return;
        searchForm.dataset.searchBound = 'true';

        if (sortInput && pageSortSelect) {
            sortInput.value = pageSortSelect.value;
        }

        // ── 1. Salary Slider Configuration ──
        async function initSalaryRange() {
            try {
                const res = await fetch('/api/jobs/salary-range');
                if (res.ok) {
                    const data = await res.json();
                    const maxDbSalary = parseInt(data.max) || 10000;
                    
                    minSlider.max = maxDbSalary;
                    maxSlider.max = maxDbSalary;
                    
                    // If max is still default 10000, adjust it to real DB max
                    if (parseInt(maxSlider.value) === 10000 && maxDbSalary > 10000) {
                        maxSlider.value = maxDbSalary;
                    }
                    
                    updateSliderUI();
                }
            } catch (err) {
                console.error('Failed to init salary range:', err);
            }
        }

        function updateSliderUI(e) {
            let min = parseInt(minSlider.value);
            let max = parseInt(maxSlider.value);

            if (min > max - 500) {
                if (e && e.target === minSlider) {
                    minSlider.value = max - 500;
                    min = parseInt(minSlider.value);
                } else {
                    maxSlider.value = min + 500;
                    max = parseInt(maxSlider.value);
                }
            }

            const sliderMax = parseInt(maxSlider.max) || 10000;
            const percent1 = (min / sliderMax) * 100;
            const percent2 = (max / sliderMax) * 100;

            track.style.left = percent1 + "%";
            track.style.width = (percent2 - percent1) + "%";

            salaryDisplay.textContent = `${min} - ${max} TND`;
        }

        // Fetch real min/max from DB before initializing completely
        initSalaryRange();

        [minSlider, maxSlider].forEach(slider => {
            slider.addEventListener('input', (e) => {
                updateSliderUI(e);
                debouncedFetch();
            });
        });

        // ── 2. AJAX Fetch Definition ──
        function debounce(func, wait) {
            let timeout;
            return function (...args) {
                clearTimeout(timeout);
                timeout = setTimeout(() => func.apply(this, args), wait);
            };
        }

        const debouncedFetch = debounce(fetchJobs, 380);

        // ── 3. Live Search Event Listeners ──
        [queryInput, catSelect, typeSelect].forEach(el => {
            if (el) el.addEventListener('input', debouncedFetch);
            if (el) el.addEventListener('change', debouncedFetch);
        });

        if (pageSortSelect) {
            pageSortSelect.addEventListener('change', () => {
                if (sortInput) {
                    sortInput.value = pageSortSelect.value;
                }
                fetchJobs();
            });
        }

        searchForm.addEventListener('submit', (e) => {
            e.preventDefault();
            fetchJobs();
        });

        // ── 4. The actual fetchJobs function ──
        async function fetchJobs() {
            const params = new URLSearchParams();
            params.set('q', queryInput ? queryInput.value : '');
            params.set('category', catSelect ? catSelect.value : '');
            params.set('job_type', typeSelect ? typeSelect.value : '');
            params.set('min_salary', minSlider.value);
            params.set('max_salary', maxSlider.value);
            params.set('sort', pageSortSelect ? pageSortSelect.value : sortInput ? sortInput.value : 'newest');

            if (jobWrapper) {
                jobWrapper.style.opacity = '0.5';
                jobWrapper.style.pointerEvents = 'none';
            }

            try {
                const response = await fetch(`/api/jobs/search?${params.toString()}`);
                if (!response.ok) throw new Error('Search failed');
                const jobs = await response.json();
                renderJobs(jobs);
            } catch (error) {
                console.error('Error fetching jobs:', error);
            } finally {
                if (jobWrapper) {
                    jobWrapper.style.opacity = '1';
                    jobWrapper.style.pointerEvents = 'auto';
                }
            }
        }

        function renderJobs(jobs) {
            if (!jobList) return;
            if (jobs.length === 0) {
                jobList.innerHTML = `
                    <div class="empty-state-box">
                        <div style="font-size:3rem;margin-bottom:.75rem;">🔍</div>
                        <h3 style="font-weight:700;margin-bottom:.5rem;">No jobs found</h3>
                        <p style="color:#6b7280;">Try different keywords or adjust your filters.</p>
                    </div>
                `;
                return;
            }

            jobList.innerHTML = jobs.map(job => `
                <div class="job-card" style="animation: fadeUp .4s ease both;">
                    <div style="flex:1 1 0;min-width:0;">
                        <p class="jc-title"><a href="${job.url}">${job.title}</a></p>
                        <div class="jc-meta">
                            ${job.posterName ? `<span>👤 ${job.posterName}</span>` : ''}
                            <span>🏢 ${job.company}</span>
                            <span>📍 ${job.location}</span>
                            <span>💰 ${job.salary}</span>
                            <span>🕒 ${job.postedAt}</span>
                        </div>
                        <div style="display:flex;gap:.4rem;flex-wrap:wrap;align-items:center;margin-top:.6rem;">
                            ${job.jobType ? `<span class="badge">${job.jobType}</span>` : ''}
                            <span class="badge b">${job.category}</span>
                        </div>
                    </div>
                    <div style="display:flex;flex-direction:column;align-items:flex-end;gap:.55rem;flex-shrink:0;">
                        <a class="btn-view" href="${job.url}">View Job</a>
                    </div>
                </div>
            `).join('');
        }
    }

    // Run on initial load AND after every Turbo page transition
    document.addEventListener('DOMContentLoaded', initJobSearch);
    document.addEventListener('turbo:load', initJobSearch);
})();
